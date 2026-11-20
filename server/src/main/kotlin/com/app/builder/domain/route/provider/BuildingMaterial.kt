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
import com.app.builder.domain.BuildingMaterial
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
import com.app.builder.domain.usecase.buildingmaterial.BuildingMaterialUseCases
import com.app.builder.domain.usecase.registry.RegistryUseCases

/**
 * Configures the building material routing endpoints for the server.
 * This function sets up the HTTP routes for managing building materials.
 * It delegates the core building material business logic to the provided [BuildingMaterialUseCases].
 *
 * @param permissionService The permission service.
 * @param buildingMaterialUseCases The collection of use cases handling building material logic.
 * @param registryUseCases The registry use cases used to record request telemetry.
 */
fun Route.buildingMaterialRoutes(
    permissionService: PermissionService,
    buildingMaterialUseCases: BuildingMaterialUseCases,
    registryUseCases: RegistryUseCases
) {
    authenticate {
        route(path = URL.BuildingMaterials.path) {
            get {
                if (!call.validateTime()) return@get
                if (!call.validatePermission(permissionService = permissionService, entityType = EntityType.BUILDING_MATERIAL, permission = Permission.READ)) return@get

                val queryMap = call.getQueryMap()
                val lastSyncUtc = queryMap[Query.LastSyncUtc.parameter]?.toInstant() ?: Instant.UNIX_EPOCH
                val cursorUtc = queryMap[Query.CursorUtc.parameter]?.toInstant()
                val cursorUuid = queryMap[Query.CursorUuid.parameter]?.toUuid()
                val pageSize = queryMap[Query.PageSize.parameter]?.toIntOrNull() ?: ServerConfigs.value.pageSize

                val buildingMaterials = call.getOrRespond(
                    outcome = buildingMaterialUseCases.getBuildingMaterials(lastSyncUtc = lastSyncUtc, cursorUtc = cursorUtc, cursorUuid = cursorUuid, pageSize = pageSize),
                    error = "Error getting building materials"
                ) ?: return@get
                call.respondSafely(status = HttpStatusCode.OK, message = buildingMaterials)
                Telemetry.info(tag = TAG, message = "Fetched ${buildingMaterials.size} building materials")
            }

            get(path = "/{uuid}") {
                if (!call.validateTime()) return@get
                if (!call.validatePermission(permissionService = permissionService, entityType = EntityType.BUILDING_MATERIAL, permission = Permission.READ)) return@get

                val uuid = call.getParameters()["uuid"]?.toUuid() ?: run {
                    call.respondSafely(status = HttpStatusCode.BadRequest, message = "Invalid building material uuid")
                    return@get
                }

                val buildingMaterial = call.getOrRespond(
                    outcome = buildingMaterialUseCases.getBuildingMaterial(uuid = uuid),
                    notFound = "Building material link not found",
                    error = "Error getting building material"
                ) ?: return@get
                call.respondSafely(status = HttpStatusCode.OK, message = buildingMaterial)
                Telemetry.info(tag = TAG, message = "Fetched building material $uuid")
            }

            post {
                if (!call.validateTime()) return@post
                val userUuid = call.getUserUuid() ?: return@post
                if (!call.validatePermission(permissionService = permissionService, entityType = EntityType.BUILDING_MATERIAL, permission = Permission.WRITE)) return@post
                val buildingMaterial = call.receive<BuildingMaterial>() ?: return@post
                if (buildingMaterial.quantity < 1) {
                    call.respondSafely(status = HttpStatusCode.BadRequest, message = "Invalid building material quantity")
                    return@post
                }

                call.getOrRespond(outcome = buildingMaterialUseCases.upsertBuildingMaterial(buildingMaterial = buildingMaterial), error = "Error upserting building material") ?: return@post

                call.respondSafely(status = HttpStatusCode.OK, message = buildingMaterial)
                Telemetry.info(tag = TAG, message = "Upserted building material ${buildingMaterial.uuid} for user $userUuid")

                call.toRegistry(
                    entityType = EntityType.BUILDING_MATERIAL,
                    entityUuid = buildingMaterial.uuid,
                    payload = encode(value = buildingMaterial)
                )?.let { registryUseCases.upsertRegistry(registry = it) }
            }
        }
    }
}

private const val TAG = "BuildingMaterialRoute"