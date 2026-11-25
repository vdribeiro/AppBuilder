package com.app.builder.domain.route.provider

import io.ktor.http.ContentDisposition
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.decodeURLPart
import io.ktor.server.auth.authenticate
import io.ktor.server.request.receiveChannel
import io.ktor.server.response.header
import io.ktor.server.response.respondBytesWriter
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import io.ktor.utils.io.copyAndClose
import com.app.builder.core.security.toUuid
import com.app.builder.core.telemetry.Telemetry
import com.app.builder.data.http.URL
import com.app.builder.data.serializer.encode
import com.app.builder.domain.EntityType
import com.app.builder.domain.FileMetadata
import com.app.builder.domain.Permission
import com.app.builder.domain.permission.PermissionService
import com.app.builder.domain.route.getOrRespond
import com.app.builder.domain.route.getParameters
import com.app.builder.domain.route.getUserUuid
import com.app.builder.domain.route.respondSafely
import com.app.builder.domain.route.toRegistry
import com.app.builder.domain.route.validatePermission
import com.app.builder.domain.route.validateTime
import com.app.builder.domain.usecase.file.FileUseCases
import com.app.builder.domain.usecase.registry.RegistryUseCases

/**
 * Configures the file routing endpoints for the server.
 * This function sets up the HTTP routes for uploading and downloading files.
 * It delegates the core file business logic to the provided [FileUseCases].
 *
 * @param permissionService The permission service.
 * @param fileUseCases The collection of use cases handling file logic.
 * @param registryUseCases The registry use cases used to record request telemetry.
 */
fun Route.fileRoutes(
    permissionService: PermissionService,
    fileUseCases: FileUseCases,
    registryUseCases: RegistryUseCases
) {
    authenticate {
        route(path = URL.Files.path) {
            get(path = "/{uuid}") {
                if (!call.validateTime()) return@get
                if (!call.validatePermission(permissionService = permissionService, entityType = EntityType.FILE, permission = Permission.READ)) return@get

                val uuid = call.getParameters()["uuid"]?.toUuid() ?: run {
                    call.respondSafely(status = HttpStatusCode.BadRequest, message = "Invalid file uuid")
                    return@get
                }

                val file = call.getOrRespond(
                    outcome = fileUseCases.getFile(uuid = uuid),
                    notFound = "File not found",
                    error = "Error getting file"
                ) ?: return@get
                call.respondSafely(status = HttpStatusCode.OK, message = file)
                Telemetry.info(tag = TAG, message = "Fetched file $uuid")
            }

            get(path = "/{uuid}/content") {
                if (!call.validateTime()) return@get
                if (!call.validatePermission(permissionService = permissionService, entityType = EntityType.FILE, permission = Permission.READ)) return@get

                val uuid = call.getParameters()["uuid"]?.toUuid() ?: run {
                    call.respondSafely(status = HttpStatusCode.BadRequest, message = "Invalid file uuid")
                    return@get
                }

                val file = call.getOrRespond(
                    outcome = fileUseCases.getFile(uuid = uuid),
                    notFound = "File not found",
                    error = "Error getting file"
                ) ?: return@get
                val content = call.getOrRespond(
                    outcome = fileUseCases.readFile(uuid = uuid),
                    notFound = "File content not found",
                    error = "Error reading file"
                ) ?: return@get

                call.response.header(
                    name = HttpHeaders.ContentDisposition,
                    value = ContentDisposition.Attachment.withParameter(key = ContentDisposition.Parameters.FileNameAsterisk, value = file.name).toString()
                )
                call.respondBytesWriter(
                    contentType = runCatching { ContentType.parse(value = file.contentType) }.getOrDefault(defaultValue = ContentType.Application.OctetStream),
                    status = HttpStatusCode.OK,
                    contentLength = file.size
                ) { content.copyAndClose(channel = this) }
                Telemetry.info(tag = TAG, message = "Downloaded file $uuid")
            }

            post(path = "/{uuid}") {
                if (!call.validateTime()) return@post
                val userUuid = call.getUserUuid() ?: return@post
                if (!call.validatePermission(permissionService = permissionService, entityType = EntityType.FILE, permission = Permission.WRITE)) return@post

                val uuid = call.getParameters()["uuid"]?.toUuid() ?: run {
                    call.respondSafely(status = HttpStatusCode.BadRequest, message = "Invalid file uuid")
                    return@post
                }
                val name = call.request.headers[HttpHeaders.ContentDisposition]
                    ?.let { runCatching { ContentDisposition.parse(value = it) }.getOrNull() }
                    ?.fileName()
                    ?.takeIf { it.isNotBlank() }
                    ?: run {
                        call.respondSafely(status = HttpStatusCode.BadRequest, message = "Missing file name")
                        return@post
                    }
                val contentType = call.request.headers[HttpHeaders.ContentType]
                    ?.let { runCatching { ContentType.parse(value = it).withoutParameters() }.getOrNull() }
                    ?: ContentType.Application.OctetStream

                val file = call.getOrRespond(
                    outcome = fileUseCases.uploadFile(
                        file = FileMetadata(uuid = uuid, name = name, contentType = contentType.toString()),
                        content = call.receiveChannel()
                    ),
                    error = "Error uploading file"
                ) ?: return@post

                call.respondSafely(status = HttpStatusCode.OK, message = file)
                Telemetry.info(tag = TAG, message = "Uploaded file $uuid of ${file.size} bytes for user $userUuid")

                call.toRegistry(
                    entityType = EntityType.FILE,
                    entityUuid = file.uuid,
                    payload = encode(value = file)
                )?.let { registryUseCases.upsertRegistry(registry = it) }
            }
        }
    }
}

/**
 * Reads the file name of this content disposition, preferring the `filename*` parameter over `filename`.
 * A `filename*` without a charset prefix is read as written.
 *
 * @receiver The parsed `Content-Disposition` header.
 * @return The file name, or null when there is none or `filename*` is not valid UTF-8 percent-encoding.
 */
private fun ContentDisposition.fileName(): String? {
    val extended = parameter(name = ContentDisposition.Parameters.FileNameAsterisk) ?: return parameter(name = ContentDisposition.Parameters.FileName)
    val (charset, _, value) = extended.split("'", limit = 3).takeIf { it.size == 3 } ?: return extended
    if (!charset.equals(other = "utf-8", ignoreCase = true)) return null
    return runCatching { value.decodeURLPart() }.getOrNull()
}

private const val TAG = "FileRoute"
