package com.app.builder.domain.route.provider

import kotlin.time.Duration.Companion.minutes
import io.ktor.http.HttpStatusCode
import io.ktor.server.auth.authenticate
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import com.app.builder.core.telemetry.Telemetry
import com.app.builder.data.http.URL
import com.app.builder.domain.AvatarUpload
import com.app.builder.domain.EntityType
import com.app.builder.domain.Permission
import com.app.builder.domain.permission.PermissionService
import com.app.builder.domain.route.getUserUuid
import com.app.builder.domain.route.respondSafely
import com.app.builder.domain.route.validatePermission
import com.app.builder.domain.route.validateTime
import com.app.builder.domain.storage.AvatarKey
import com.app.builder.domain.storage.ObjectStorageService

/**
 * Configures the avatar upload routing endpoint for the server.
 * The server signs an upload rather than carrying it, so image bytes never occupy the server's memory or bandwidth.
 *
 * @param permissionService The permission service.
 * @param objectStorageService The object storage the upload is signed against.
 */
fun Route.avatarRoutes(
    permissionService: PermissionService,
    objectStorageService: ObjectStorageService
) {
    authenticate {
        post(path = URL.AvatarUpload.path) {
            if (!call.validateTime()) return@post
            val userUuid = call.getUserUuid() ?: return@post
            if (!call.validatePermission(permissionService = permissionService, entityType = EntityType.USER, permission = Permission.WRITE)) return@post

            val key = AvatarKey.create(userUuid = userUuid)
            val url = objectStorageService.presignUpload(key = key, contentType = AvatarKey.CONTENT_TYPE, expiry = UPLOAD_EXPIRY) ?: run {
                Telemetry.error(tag = TAG, message = "Unable to sign an avatar upload for user $userUuid")
                call.respondSafely(status = HttpStatusCode.ServiceUnavailable, message = "Object storage is unavailable")
                return@post
            }

            call.respondSafely(status = HttpStatusCode.OK, message = AvatarUpload(key = key, url = url, contentType = AvatarKey.CONTENT_TYPE))
            Telemetry.info(tag = TAG, message = "Granted an avatar upload to user $userUuid")
        }
    }
}

private const val TAG = "AvatarRoute"

/** How long a granted upload stays valid, long enough to send an image over a slow connection and no longer. */
private val UPLOAD_EXPIRY = 5.minutes
