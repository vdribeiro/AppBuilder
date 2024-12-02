package com.app.builder.domain.route.provider

import io.ktor.http.HttpStatusCode
import io.ktor.server.auth.authenticate
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import com.app.builder.core.telemetry.Telemetry
import com.app.builder.data.http.URL
import com.app.builder.data.serializer.encode
import com.app.builder.domain.DeviceLocation
import com.app.builder.domain.EntityType
import com.app.builder.domain.Permission
import com.app.builder.domain.permission.PermissionService
import com.app.builder.domain.route.getUserUuid
import com.app.builder.domain.route.receive
import com.app.builder.domain.route.respondSafely
import com.app.builder.domain.route.toRegistry
import com.app.builder.domain.route.validatePermission
import com.app.builder.domain.route.validateTime
import com.app.builder.domain.usecase.devicelocation.DeviceLocationUseCases
import com.app.builder.domain.usecase.registry.RegistryUseCases

/**
 * Configures the device location routing endpoint for the server.
 * This allows a device to report a captured location fix.
 *
 * @param permissionService The permission service.
 * @param deviceLocationUseCases The use cases handling device location persistence.
 * @param registryUseCases The registry use cases used to record request telemetry.
 */
fun Route.deviceLocationRoutes(
    permissionService: PermissionService,
    deviceLocationUseCases: DeviceLocationUseCases,
    registryUseCases: RegistryUseCases
) {
    authenticate {
        post(path = URL.DeviceLocations.path) {
            if (!call.validateTime()) return@post
            val userUuid = call.getUserUuid() ?: return@post
            val deviceLocation = call.receive<DeviceLocation>() ?: return@post
            if (!call.validatePermission(permissionService = permissionService, entityType = EntityType.DEVICE_LOCATION, permission = Permission.WRITE)) return@post
            if (deviceLocation.latitude !in -90.0..90.0 || deviceLocation.longitude !in -180.0..180.0) {
                call.respondSafely(status = HttpStatusCode.BadRequest, message = "Invalid device location parameters")
                return@post
            }

            val success = deviceLocationUseCases.upsertDeviceLocation(userUuid = userUuid, deviceLocation = deviceLocation)
            if (!success) {
                Telemetry.error(tag = TAG, message = "User $userUuid is not authorized to upsert device location ${deviceLocation.uuid}")
                call.respondSafely(status = HttpStatusCode.Forbidden, message = "Not authorized to modify this task")
                return@post
            }

            call.respondSafely(status = HttpStatusCode.OK, message = "Device location upserted")
            Telemetry.info(tag = TAG, message = "Upserted device location ${deviceLocation.uuid} for user $userUuid")

            call.toRegistry(
                entityType = EntityType.DEVICE_LOCATION,
                entityUuid = deviceLocation.uuid,
                payload = encode(value = deviceLocation)
            )?.let { registryUseCases.upsertRegistry(registry = it) }
        }
    }
}

private const val TAG = "DeviceLocationRoute"
