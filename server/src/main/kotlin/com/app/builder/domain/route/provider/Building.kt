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
import com.app.builder.domain.Building
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
import com.app.builder.domain.usecase.registry.RegistryUseCases
import com.app.builder.domain.usecase.building.BuildingUseCases

/**
 * Configures the building routing endpoints for the server.
 * This function sets up the HTTP routes for managing buildings.
 * It delegates the core building business logic to the provided [BuildingUseCases].
 *
 * @param permissionService The permission service.
 * @param buildingUseCases The collection of use cases handling building logic.
 * @param registryUseCases The registry use cases used to record request telemetry.
 */
fun Route.buildingRoutes(
    permissionService: PermissionService,
    buildingUseCases: BuildingUseCases,
    registryUseCases: RegistryUseCases
) {
    authenticate {
        route(path = URL.Buildings.path) {
            get {
                if (!call.validateTime()) return@get
                if (!call.validatePermission(permissionService = permissionService, entityType = EntityType.BUILDING, permission = Permission.READ)) return@get

                val queryMap = call.getQueryMap()
                val lastSyncUtc = queryMap[Query.LastSyncUtc.parameter]?.toInstant() ?: Instant.fromEpochSeconds(epochSeconds = 0)
                val cursorUtc = queryMap[Query.CursorUtc.parameter]?.toInstant()
                val cursorUuid = queryMap[Query.CursorUuid.parameter]?.toUuid()
                val pageSize = queryMap[Query.PageSize.parameter]?.toIntOrNull() ?: ServerConfigs.value.pageSize

                val buildings = call.getOrRespond(
                    outcome = buildingUseCases.getBuildings(lastSyncUtc = lastSyncUtc, cursorUtc = cursorUtc, cursorUuid = cursorUuid, pageSize = pageSize),
                    error = "Error getting buildings"
                ) ?: return@get
                call.respondSafely(status = HttpStatusCode.OK, message = buildings)
                Telemetry.info(tag = TAG, message = "Fetched ${buildings.size} buildings")

                call.toRegistry(entityType = EntityType.BUILDING)?.let { registryUseCases.upsertRegistry(registry = it) }
            }

            get(path = "/{uuid}") {
                if (!call.validateTime()) return@get
                if (!call.validatePermission(permissionService = permissionService, entityType = EntityType.BUILDING, permission = Permission.READ)) return@get

                val uuid = call.getParameters()["uuid"]?.toUuid() ?: run {
                    call.respondSafely(status = HttpStatusCode.BadRequest, message = "Invalid building uuid")
                    return@get
                }

                val building = call.getOrRespond(
                    outcome = buildingUseCases.getBuilding(uuid = uuid),
                    notFound = "Building not found",
                    error = "Error getting building"
                ) ?: return@get
                call.respondSafely(status = HttpStatusCode.OK, message = building)
                Telemetry.info(tag = TAG, message = "Fetched building $uuid")

                call.toRegistry(
                    entityType = EntityType.BUILDING,
                    entityUuid = uuid
                )?.let { registryUseCases.upsertRegistry(registry = it) }
            }

            post {
                if (!call.validateTime()) return@post
                val userUuid = call.getUserUuid() ?: return@post
                if (!call.validatePermission(permissionService = permissionService, entityType = EntityType.BUILDING, permission = Permission.WRITE)) return@post
                val building = call.receive<Building>() ?: return@post
                val latitude = building.latitude
                val longitude = building.longitude
                val validCoordinates = when {
                    latitude == null && longitude == null -> true
                    latitude == null || longitude == null -> false
                    else -> latitude in -90.0..90.0 && longitude in -180.0..180.0
                }
                if (!validCoordinates) {
                    call.respondSafely(status = HttpStatusCode.BadRequest, message = "Invalid building coordinates")
                    return@post
                }

                call.getOrRespond(outcome = buildingUseCases.upsertBuilding(building = building), error = "Error upserting building") ?: return@post

                call.respondSafely(status = HttpStatusCode.OK, message = building)
                Telemetry.info(tag = TAG, message = "Upserted building ${building.uuid} for user $userUuid")

                call.toRegistry(
                    entityType = EntityType.BUILDING,
                    entityUuid = building.uuid,
                    payload = encode(value = building)
                )?.let { registryUseCases.upsertRegistry(registry = it) }
            }
        }
    }
}

private const val TAG = "BuildingRoute"