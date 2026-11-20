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
import com.app.builder.domain.EquipmentUser
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
import com.app.builder.domain.usecase.equipmentuser.EquipmentUserUseCases
import com.app.builder.domain.usecase.registry.RegistryUseCases

/**
 * Configures the equipment user routing endpoints for the server.
 * This function sets up the HTTP routes for managing equipment users.
 * It delegates the core equipment user business logic to the provided [EquipmentUserUseCases].
 *
 * @param permissionService The permission service.
 * @param equipmentUserUseCases The collection of use cases handling equipment user logic.
 * @param registryUseCases The registry use cases used to record request telemetry.
 */
fun Route.equipmentUserRoutes(
    permissionService: PermissionService,
    equipmentUserUseCases: EquipmentUserUseCases,
    registryUseCases: RegistryUseCases
) {
    authenticate {
        route(path = URL.EquipmentUsers.path) {
            get {
                if (!call.validateTime()) return@get
                if (!call.validatePermission(permissionService = permissionService, entityType = EntityType.EQUIPMENT_USER, permission = Permission.READ)) return@get

                val queryMap = call.getQueryMap()
                val lastSyncUtc = queryMap[Query.LastSyncUtc.parameter]?.toInstant() ?: Instant.UNIX_EPOCH
                val cursorUtc = queryMap[Query.CursorUtc.parameter]?.toInstant()
                val cursorUuid = queryMap[Query.CursorUuid.parameter]?.toUuid()
                val pageSize = queryMap[Query.PageSize.parameter]?.toIntOrNull() ?: ServerConfigs.value.pageSize

                val equipmentUsers = call.getOrRespond(
                    outcome = equipmentUserUseCases.getEquipmentUsers(lastSyncUtc = lastSyncUtc, cursorUtc = cursorUtc, cursorUuid = cursorUuid, pageSize = pageSize),
                    error = "Error getting equipment users"
                ) ?: return@get
                call.respondSafely(status = HttpStatusCode.OK, message = equipmentUsers)
                Telemetry.info(tag = TAG, message = "Fetched ${equipmentUsers.size} equipment users")
            }

            get(path = "/{uuid}") {
                if (!call.validateTime()) return@get
                if (!call.validatePermission(permissionService = permissionService, entityType = EntityType.EQUIPMENT_USER, permission = Permission.READ)) return@get

                val uuid = call.getParameters()["uuid"]?.toUuid() ?: run {
                    call.respondSafely(status = HttpStatusCode.BadRequest, message = "Invalid equipment user uuid")
                    return@get
                }

                val equipmentUser = call.getOrRespond(
                    outcome = equipmentUserUseCases.getEquipmentUser(uuid = uuid),
                    notFound = "Equipment user link not found",
                    error = "Error getting equipment user"
                ) ?: return@get
                call.respondSafely(status = HttpStatusCode.OK, message = equipmentUser)
                Telemetry.info(tag = TAG, message = "Fetched equipment user $uuid")
            }

            post {
                if (!call.validateTime()) return@post
                val userUuid = call.getUserUuid() ?: return@post
                if (!call.validatePermission(permissionService = permissionService, entityType = EntityType.EQUIPMENT_USER, permission = Permission.WRITE)) return@post
                val equipmentUser = call.receive<EquipmentUser>() ?: return@post

                call.getOrRespond(outcome = equipmentUserUseCases.upsertEquipmentUser(equipmentUser = equipmentUser), error = "Error upserting equipment user") ?: return@post

                call.respondSafely(status = HttpStatusCode.OK, message = equipmentUser)
                Telemetry.info(tag = TAG, message = "Upserted equipment user ${equipmentUser.uuid} for user $userUuid")

                call.toRegistry(
                    entityType = EntityType.EQUIPMENT_USER,
                    entityUuid = equipmentUser.uuid,
                    payload = encode(value = equipmentUser)
                )?.let { registryUseCases.upsertRegistry(registry = it) }
            }
        }
    }
}

private const val TAG = "EquipmentUserRoute"