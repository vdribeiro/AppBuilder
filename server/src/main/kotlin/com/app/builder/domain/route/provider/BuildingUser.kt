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
import com.app.builder.domain.BuildingUser
import com.app.builder.domain.EntityType
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
import com.app.builder.domain.usecase.buildinguser.BuildingUserUseCases
import com.app.builder.domain.usecase.registry.RegistryUseCases

/**
 * Configures the building user routing endpoints for the server.
 * This function sets up the HTTP routes for managing building users.
 * It delegates the core building user business logic to the provided [BuildingUserUseCases].
 *
 * @param permissionService The permission service.
 * @param buildingUserUseCases The collection of use cases handling building user logic.
 * @param registryUseCases The registry use cases used to record request telemetry.
 */
fun Route.buildingUserRoutes(
    permissionService: PermissionService,
    buildingUserUseCases: BuildingUserUseCases,
    registryUseCases: RegistryUseCases
) {
    authenticate {
        route(path = URL.BuildingUsers.path) {
            get {
                if (!call.validateTime()) return@get
                if (!call.validatePermission(permissionService = permissionService, entityType = EntityType.BUILDING_USER, permission = Permission.READ)) return@get

                val queryMap = call.getQueryMap()
                val lastSyncUtc = queryMap[Query.LastSyncUtc.parameter]?.toInstant() ?: Instant.UNIX_EPOCH
                val cursorUtc = queryMap[Query.CursorUtc.parameter]?.toInstant()
                val cursorUuid = queryMap[Query.CursorUuid.parameter]?.toUuid()
                val pageSize = queryMap[Query.PageSize.parameter]?.toIntOrNull() ?: ServerConfigs.value.pageSize

                val buildingUsers = call.getOrRespond(
                    outcome = buildingUserUseCases.getBuildingUsers(lastSyncUtc = lastSyncUtc, cursorUtc = cursorUtc, cursorUuid = cursorUuid, pageSize = pageSize),
                    error = "Error getting building users"
                ) ?: return@get
                call.respondSafely(status = HttpStatusCode.OK, message = buildingUsers)
                Telemetry.info(tag = TAG, message = "Fetched ${buildingUsers.size} building users")
            }

            get(path = "/{uuid}") {
                if (!call.validateTime()) return@get
                if (!call.validatePermission(permissionService = permissionService, entityType = EntityType.BUILDING_USER, permission = Permission.READ)) return@get

                val uuid = call.getParameters()["uuid"]?.toUuid() ?: run {
                    call.respondSafely(status = HttpStatusCode.BadRequest, message = "Invalid building user uuid")
                    return@get
                }

                val buildingUser = call.getOrRespond(
                    outcome = buildingUserUseCases.getBuildingUser(uuid = uuid),
                    notFound = "Building user link not found",
                    error = "Error getting building user"
                ) ?: return@get
                call.respondSafely(status = HttpStatusCode.OK, message = buildingUser)
                Telemetry.info(tag = TAG, message = "Fetched building user $uuid")
            }

            post {
                if (!call.validateTime()) return@post
                val userUuid = call.getUserUuid() ?: return@post
                if (!call.validatePermission(permissionService = permissionService, entityType = EntityType.BUILDING_USER, permission = Permission.WRITE)) return@post
                val buildingUser = call.receive<BuildingUser>() ?: return@post

                call.getOrRespond(outcome = buildingUserUseCases.upsertBuildingUser(buildingUser = buildingUser), error = "Error upserting building user") ?: return@post

                call.respondSafely(status = HttpStatusCode.OK, message = buildingUser)
                Telemetry.info(tag = TAG, message = "Upserted building user ${buildingUser.uuid} for user $userUuid")

                call.toRegistry(
                    entityType = EntityType.BUILDING_USER,
                    entityUuid = buildingUser.uuid,
                    payload = encode(value = buildingUser)
                )?.let { registryUseCases.upsertRegistry(registry = it) }
            }
        }
    }
}

private const val TAG = "BuildingUserRoute"