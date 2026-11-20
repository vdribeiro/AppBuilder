package com.app.builder.domain.route.provider

import kotlin.time.Instant
import io.ktor.http.HttpStatusCode
import io.ktor.server.auth.authenticate
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import com.app.builder.core.config.ServerConfigs
import com.app.builder.core.locale.UNIX_EPOCH
import com.app.builder.core.locale.toInstant
import com.app.builder.core.security.toUuid
import com.app.builder.core.telemetry.Telemetry
import com.app.builder.data.http.Query
import com.app.builder.data.http.URL
import com.app.builder.data.serializer.encode
import com.app.builder.domain.EntityType
import com.app.builder.domain.Material
import com.app.builder.domain.Permission
import com.app.builder.domain.permission.PermissionService
import com.app.builder.domain.route.getOrRespond
import com.app.builder.domain.route.getParameters
import com.app.builder.domain.route.getQueryMap
import com.app.builder.domain.route.getUserUuid
import com.app.builder.domain.route.receive
import com.app.builder.domain.route.respondSafely
import com.app.builder.domain.route.toRegistry
import com.app.builder.domain.route.validatePermission
import com.app.builder.domain.route.validateTime
import com.app.builder.domain.usecase.material.MaterialUseCases
import com.app.builder.domain.usecase.registry.RegistryUseCases

/**
 * Configures the material routing endpoints for the server.
 * This function sets up the HTTP routes for managing materials.
 * It delegates the core material business logic to the provided [MaterialUseCases].
 *
 * @param permissionService The permission service.
 * @param materialUseCases The collection of use cases handling material logic.
 * @param registryUseCases The registry use cases used to record request telemetry.
 */
fun Route.materialRoutes(
    permissionService: PermissionService,
    materialUseCases: MaterialUseCases,
    registryUseCases: RegistryUseCases
) {
    authenticate {
        route(path = URL.Materials.path) {
            get {
                if (!call.validateTime()) return@get
                if (!call.validatePermission(permissionService = permissionService, entityType = EntityType.MATERIAL, permission = Permission.READ)) return@get

                val queryMap = call.getQueryMap()
                val lastSyncUtc = queryMap[Query.LastSyncUtc.parameter]?.toInstant() ?: Instant.UNIX_EPOCH
                val cursorUtc = queryMap[Query.CursorUtc.parameter]?.toInstant()
                val cursorUuid = queryMap[Query.CursorUuid.parameter]?.toUuid()
                val pageSize = queryMap[Query.PageSize.parameter]?.toIntOrNull() ?: ServerConfigs.value.pageSize

                val materials = call.getOrRespond(
                    outcome = materialUseCases.getMaterials(lastSyncUtc = lastSyncUtc, cursorUtc = cursorUtc, cursorUuid = cursorUuid, pageSize = pageSize),
                    error = "Error getting materials"
                ) ?: return@get
                call.respondSafely(status = HttpStatusCode.OK, message = materials)
                Telemetry.info(tag = TAG, message = "Fetched ${materials.size} materials")
            }

            get(path = "/{uuid}") {
                if (!call.validateTime()) return@get
                if (!call.validatePermission(permissionService = permissionService, entityType = EntityType.MATERIAL, permission = Permission.READ)) return@get

                val uuid = call.getParameters()["uuid"]?.toUuid() ?: run {
                    call.respondSafely(status = HttpStatusCode.BadRequest, message = "Invalid material uuid")
                    return@get
                }

                val material = call.getOrRespond(
                    outcome = materialUseCases.getMaterial(uuid = uuid),
                    notFound = "Material not found",
                    error = "Error getting material"
                ) ?: return@get
                call.respondSafely(status = HttpStatusCode.OK, message = material)
                Telemetry.info(tag = TAG, message = "Fetched material $uuid")
            }

            post {
                if (!call.validateTime()) return@post
                val userUuid = call.getUserUuid() ?: return@post
                if (!call.validatePermission(permissionService = permissionService, entityType = EntityType.MATERIAL, permission = Permission.WRITE)) return@post
                val material = call.receive<Material>() ?: return@post

                call.getOrRespond(outcome = materialUseCases.upsertMaterial(material = material), error = "Error upserting material") ?: return@post

                call.respondSafely(status = HttpStatusCode.OK, message = material)
                Telemetry.info(tag = TAG, message = "Upserted material ${material.uuid} for user $userUuid")

                call.toRegistry(
                    entityType = EntityType.MATERIAL,
                    entityUuid = material.uuid,
                    payload = encode(value = material)
                )?.let { registryUseCases.upsertRegistry(registry = it) }
            }
        }
    }
}

private const val TAG = "MaterialRoute"