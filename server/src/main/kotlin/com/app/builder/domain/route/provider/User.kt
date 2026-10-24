package com.app.builder.domain.route.provider

import kotlin.time.Instant
import io.ktor.http.HttpStatusCode
import io.ktor.server.auth.authenticate
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import com.app.builder.core.config.ServerConfigs
import com.app.builder.core.locale.toInstant
import com.app.builder.core.security.toUuid
import com.app.builder.core.telemetry.Telemetry
import com.app.builder.data.http.Query
import com.app.builder.data.http.URL
import com.app.builder.data.serializer.encode
import com.app.builder.domain.EntityType
import com.app.builder.domain.Permission
import com.app.builder.domain.User
import com.app.builder.domain.permission.PermissionService
import com.app.builder.domain.storage.AvatarKey
import com.app.builder.domain.storage.ObjectStorageService
import com.app.builder.domain.permission.UserAccess
import com.app.builder.domain.route.getParameters
import com.app.builder.domain.route.getAccess
import com.app.builder.domain.route.getQueryMap
import com.app.builder.domain.route.getUserUuid
import com.app.builder.domain.route.receive
import com.app.builder.domain.route.respondSafely
import com.app.builder.domain.route.toRegistry
import com.app.builder.domain.route.validatePermission
import com.app.builder.domain.route.validateTime
import com.app.builder.domain.usecase.registry.RegistryUseCases
import com.app.builder.domain.usecase.user.UserUseCases

/**
 * Configures the user routes.
 *
 * @param permissionService The permission service.
 * @param userUseCases The collection of use cases handling user logic.
 * @param registryUseCases The registry use cases used to record request telemetry.
 */
fun Route.userRoutes(
    permissionService: PermissionService,
    userUseCases: UserUseCases,
    registryUseCases: RegistryUseCases,
    objectStorageService: ObjectStorageService
) {
    authenticate {
        route(path = URL.Users.path) {
            get {
                if (!call.validatePermission(permissionService = permissionService, entityType = EntityType.USER, permission = Permission.READ)) return@get

                val queryMap = call.getQueryMap()
                val lastSyncUtc = queryMap[Query.LastSyncUtc.parameter]?.toInstant() ?: Instant.fromEpochSeconds(epochSeconds = 0)
                val cursorUtc = queryMap[Query.CursorUtc.parameter]?.toInstant()
                val cursorUuid = queryMap[Query.CursorUuid.parameter]?.toUuid()
                val pageSize = queryMap[Query.PageSize.parameter]?.toIntOrNull() ?: ServerConfigs.configs.pageSize

                val users = userUseCases.getUsers(lastSyncUtc = lastSyncUtc, cursorUtc = cursorUtc, cursorUuid = cursorUuid, pageSize = pageSize)
                call.respondSafely(status = HttpStatusCode.OK, message = users)
                Telemetry.info(tag = TAG, message = "Fetched ${users.size} users")

                call.toRegistry(entityType = EntityType.USER)?.let { registryUseCases.upsertRegistry(registry = it) }
            }

            get(path = "/{uuid}") {
                if (!call.validatePermission(permissionService = permissionService, entityType = EntityType.USER, permission = Permission.READ)) return@get

                val uuid = call.getParameters()["uuid"]?.toUuid() ?: run {
                    call.respondSafely(status = HttpStatusCode.BadRequest, message = "Invalid user uuid")
                    return@get
                }

                val user = userUseCases.getUser(uuid = uuid) ?: run {
                    call.respondSafely(status = HttpStatusCode.NotFound, message = "User not found")
                    return@get
                }
                call.respondSafely(status = HttpStatusCode.OK, message = user)
                Telemetry.info(tag = TAG, message = "Fetched user $uuid")

                call.toRegistry(
                    entityType = EntityType.USER,
                    entityUuid = uuid
                )?.let { registryUseCases.upsertRegistry(registry = it) }
            }

            post {
                if (!call.validateTime()) return@post
                val userUuid = call.getUserUuid() ?: return@post
                if (!call.validatePermission(permissionService = permissionService, entityType = EntityType.USER, permission = Permission.WRITE)) return@post
                val user = call.receive<User>() ?: return@post

                // Users can only change themselves, unless it also has session write permission
                val hasPermission = call.getAccess(permissionService = permissionService)?.permissions?.get(EntityType.SESSION) == Permission.WRITE
                if (user.uuid != userUuid && !hasPermission) {
                    call.respondSafely(status = HttpStatusCode.Forbidden, message = "Not authorized to modify user ${user.uuid}")
                    return@post
                }
                val storedUser = userUseCases.getUser(uuid = user.uuid)
                // Also, only a user with session write permission can change user permissions or the deleted state.
                val validatedUser = if (hasPermission) user else {
                    val dbUser = storedUser ?: run {
                        Telemetry.error(tag = TAG, message = "Unable to get user ${user.uuid}")
                        call.respondSafely(status = HttpStatusCode.InternalServerError, message = "Error getting user ${user.uuid}")
                        return@post
                    }
                    user.copy(permissions = dbUser.permissions, deletedAt = dbUser.deletedAt)
                }

                // A claimed avatar is only accepted once the image is actually in the bucket and sits in this user's own folder,
                // so the column cannot come to point at a missing object or at somebody else's image.
                val previousAvatar = storedUser?.avatar
                val avatar = validatedUser.avatar
                if (avatar != null && avatar != previousAvatar) {
                    if (!AvatarKey.belongsTo(key = avatar, userUuid = validatedUser.uuid)) {
                        call.respondSafely(status = HttpStatusCode.Forbidden, message = "Not authorized to claim avatar $avatar")
                        return@post
                    }
                    if (!objectStorageService.exists(key = avatar)) {
                        call.respondSafely(status = HttpStatusCode.BadRequest, message = "Avatar $avatar was not uploaded")
                        return@post
                    }
                }

                val success = userUseCases.upsertUser(user = validatedUser)
                if (!success) {
                    Telemetry.error(tag = TAG, message = "Error upserting user ${validatedUser.uuid}")
                    call.respondSafely(status = HttpStatusCode.InternalServerError, message = "Error upserting user")
                    return@post
                }
                if (hasPermission) permissionService.set(userUuid = validatedUser.uuid, access = UserAccess(permissions = validatedUser.permissions, deletedAt = validatedUser.deletedAt))

                // The replaced image is no longer reachable through any user, so it is removed rather than left to accumulate in the bucket.
                if (previousAvatar != null && previousAvatar != validatedUser.avatar) objectStorageService.delete(key = previousAvatar)

                call.respondSafely(status = HttpStatusCode.OK, message = validatedUser)
                Telemetry.info(tag = TAG, message = "Upserted user ${validatedUser.uuid}")

                call.toRegistry(
                    entityType = EntityType.USER,
                    entityUuid = validatedUser.uuid,
                    payload = encode(value = validatedUser)
                )?.let { registryUseCases.upsertRegistry(registry = it) }
            }
        }
    }
}

private const val TAG = "UserRoute"