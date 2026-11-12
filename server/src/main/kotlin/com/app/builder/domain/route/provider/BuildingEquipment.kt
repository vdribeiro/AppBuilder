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
import com.app.builder.domain.BuildingEquipment
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
import com.app.builder.domain.usecase.buildingequipment.BuildingEquipmentUseCases
import com.app.builder.domain.usecase.registry.RegistryUseCases

/**
 * Configures the building equipment routing endpoints for the server.
 * This function sets up the HTTP routes for managing building equipments.
 * It delegates the core building equipment business logic to the provided [BuildingEquipmentUseCases].
 *
 * @param permissionService The permission service.
 * @param buildingEquipmentUseCases The collection of use cases handling building equipment logic.
 * @param registryUseCases The registry use cases used to record request telemetry.
 */
fun Route.buildingEquipmentRoutes(
    permissionService: PermissionService,
    buildingEquipmentUseCases: BuildingEquipmentUseCases,
    registryUseCases: RegistryUseCases
) {
    authenticate {
        route(path = URL.BuildingEquipments.path) {
            get {
                if (!call.validateTime()) return@get
                if (!call.validatePermission(permissionService = permissionService, entityType = EntityType.BUILDING_EQUIPMENT, permission = Permission.READ)) return@get

                val queryMap = call.getQueryMap()
                val lastSyncUtc = queryMap[Query.LastSyncUtc.parameter]?.toInstant() ?: Instant.fromEpochSeconds(epochSeconds = 0)
                val cursorUtc = queryMap[Query.CursorUtc.parameter]?.toInstant()
                val cursorUuid = queryMap[Query.CursorUuid.parameter]?.toUuid()
                val pageSize = queryMap[Query.PageSize.parameter]?.toIntOrNull() ?: ServerConfigs.value.pageSize

                val buildingEquipments = call.getOrRespond(
                    outcome = buildingEquipmentUseCases.getBuildingEquipments(lastSyncUtc = lastSyncUtc, cursorUtc = cursorUtc, cursorUuid = cursorUuid, pageSize = pageSize),
                    error = "Error getting building equipments"
                ) ?: return@get
                call.respondSafely(status = HttpStatusCode.OK, message = buildingEquipments)
                Telemetry.info(tag = TAG, message = "Fetched ${buildingEquipments.size} building equipments")

                call.toRegistry(entityType = EntityType.BUILDING_EQUIPMENT)?.let { registryUseCases.upsertRegistry(registry = it) }
            }

            get(path = "/{uuid}") {
                if (!call.validateTime()) return@get
                if (!call.validatePermission(permissionService = permissionService, entityType = EntityType.BUILDING_EQUIPMENT, permission = Permission.READ)) return@get

                val uuid = call.getParameters()["uuid"]?.toUuid() ?: run {
                    call.respondSafely(status = HttpStatusCode.BadRequest, message = "Invalid building equipment uuid")
                    return@get
                }

                val buildingEquipment = call.getOrRespond(
                    outcome = buildingEquipmentUseCases.getBuildingEquipment(uuid = uuid),
                    notFound = "Building equipment link not found",
                    error = "Error getting building equipment"
                ) ?: return@get
                call.respondSafely(status = HttpStatusCode.OK, message = buildingEquipment)
                Telemetry.info(tag = TAG, message = "Fetched building equipment $uuid")

                call.toRegistry(
                    entityType = EntityType.BUILDING_EQUIPMENT,
                    entityUuid = uuid
                )?.let { registryUseCases.upsertRegistry(registry = it) }
            }

            post {
                if (!call.validateTime()) return@post
                val userUuid = call.getUserUuid() ?: return@post
                if (!call.validatePermission(permissionService = permissionService, entityType = EntityType.BUILDING_EQUIPMENT, permission = Permission.WRITE)) return@post
                val buildingEquipment = call.receive<BuildingEquipment>() ?: return@post

                call.getOrRespond(outcome = buildingEquipmentUseCases.upsertBuildingEquipment(buildingEquipment = buildingEquipment), error = "Error upserting building equipment") ?: return@post

                call.respondSafely(status = HttpStatusCode.OK, message = buildingEquipment)
                Telemetry.info(tag = TAG, message = "Upserted building equipment ${buildingEquipment.uuid} for user $userUuid")

                call.toRegistry(
                    entityType = EntityType.BUILDING_EQUIPMENT,
                    entityUuid = buildingEquipment.uuid,
                    payload = encode(value = buildingEquipment)
                )?.let { registryUseCases.upsertRegistry(registry = it) }
            }
        }
    }
}

private const val TAG = "BuildingEquipmentRoute"