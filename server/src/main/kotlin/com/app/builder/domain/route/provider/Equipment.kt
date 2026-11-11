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
import com.app.builder.domain.Equipment
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
import com.app.builder.domain.usecase.equipment.EquipmentUseCases

/**
 * Configures the equipment routing endpoints for the server.
 * This function sets up the HTTP routes for managing equipments.
 * It delegates the core equipment business logic to the provided [EquipmentUseCases].
 *
 * @param permissionService The permission service.
 * @param equipmentUseCases The collection of use cases handling equipment logic.
 * @param registryUseCases The registry use cases used to record request telemetry.
 */
fun Route.equipmentRoutes(
    permissionService: PermissionService,
    equipmentUseCases: EquipmentUseCases,
    registryUseCases: RegistryUseCases
) {
    authenticate {
        route(path = URL.Equipments.path) {
            get {
                if (!call.validateTime()) return@get
                if (!call.validatePermission(permissionService = permissionService, entityType = EntityType.EQUIPMENT, permission = Permission.READ)) return@get

                val queryMap = call.getQueryMap()
                val lastSyncUtc = queryMap[Query.LastSyncUtc.parameter]?.toInstant() ?: Instant.fromEpochSeconds(epochSeconds = 0)
                val cursorUtc = queryMap[Query.CursorUtc.parameter]?.toInstant()
                val cursorUuid = queryMap[Query.CursorUuid.parameter]?.toUuid()
                val pageSize = queryMap[Query.PageSize.parameter]?.toIntOrNull() ?: ServerConfigs.value.pageSize

                val equipments = call.getOrRespond(
                    outcome = equipmentUseCases.getEquipments(lastSyncUtc = lastSyncUtc, cursorUtc = cursorUtc, cursorUuid = cursorUuid, pageSize = pageSize),
                    error = "Error getting equipments"
                ) ?: return@get
                call.respondSafely(status = HttpStatusCode.OK, message = equipments)
                Telemetry.info(tag = TAG, message = "Fetched ${equipments.size} equipments")

                call.toRegistry(entityType = EntityType.EQUIPMENT)?.let { registryUseCases.upsertRegistry(registry = it) }
            }

            get(path = "/{uuid}") {
                if (!call.validateTime()) return@get
                if (!call.validatePermission(permissionService = permissionService, entityType = EntityType.EQUIPMENT, permission = Permission.READ)) return@get

                val uuid = call.getParameters()["uuid"]?.toUuid() ?: run {
                    call.respondSafely(status = HttpStatusCode.BadRequest, message = "Invalid equipment uuid")
                    return@get
                }

                val equipment = call.getOrRespond(
                    outcome = equipmentUseCases.getEquipment(uuid = uuid),
                    notFound = "Equipment not found",
                    error = "Error getting equipment"
                ) ?: return@get
                call.respondSafely(status = HttpStatusCode.OK, message = equipment)
                Telemetry.info(tag = TAG, message = "Fetched equipment $uuid")

                call.toRegistry(
                    entityType = EntityType.EQUIPMENT,
                    entityUuid = uuid
                )?.let { registryUseCases.upsertRegistry(registry = it) }
            }

            post {
                if (!call.validateTime()) return@post
                val userUuid = call.getUserUuid() ?: return@post
                if (!call.validatePermission(permissionService = permissionService, entityType = EntityType.EQUIPMENT, permission = Permission.WRITE)) return@post
                val equipment = call.receive<Equipment>() ?: return@post

                call.getOrRespond(outcome = equipmentUseCases.upsertEquipment(equipment = equipment), error = "Error upserting equipment") ?: return@post

                call.respondSafely(status = HttpStatusCode.OK, message = equipment)
                Telemetry.info(tag = TAG, message = "Upserted equipment ${equipment.uuid} for user $userUuid")

                call.toRegistry(
                    entityType = EntityType.EQUIPMENT,
                    entityUuid = equipment.uuid,
                    payload = encode(value = equipment)
                )?.let { registryUseCases.upsertRegistry(registry = it) }
            }
        }
    }
}

private const val TAG = "EquipmentRoute"