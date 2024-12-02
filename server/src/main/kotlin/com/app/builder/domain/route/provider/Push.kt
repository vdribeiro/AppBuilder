package com.app.builder.domain.route.provider

import kotlinx.coroutines.channels.consumeEach
import io.ktor.http.HttpStatusCode
import io.ktor.server.auth.authenticate
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import io.ktor.server.websocket.webSocket
import io.ktor.websocket.CloseReason
import io.ktor.websocket.Frame
import io.ktor.websocket.close
import io.ktor.websocket.readText
import com.app.builder.core.security.toUuid
import com.app.builder.core.telemetry.Telemetry
import com.app.builder.data.http.Query
import com.app.builder.data.http.URL
import com.app.builder.data.serializer.encode
import com.app.builder.domain.DeviceToken
import com.app.builder.domain.EntityType
import com.app.builder.domain.Permission
import com.app.builder.domain.PushPayload
import com.app.builder.domain.permission.PermissionService
import com.app.builder.domain.push.PushService
import com.app.builder.domain.route.getQueryMap
import com.app.builder.domain.route.getUserUuid
import com.app.builder.domain.route.receive
import com.app.builder.domain.route.respondSafely
import com.app.builder.domain.route.toRegistry
import com.app.builder.domain.route.validatePermission
import com.app.builder.domain.usecase.devicetoken.DeviceTokenUseCases
import com.app.builder.domain.usecase.registry.RegistryUseCases

/**
 * Configures the push routing endpoints for the server.
 * This allows devices to register their FCM token and set up the WebSocket route for user push payloads.
 *
 * @param permissionService The permission service.
 * @param deviceTokenUseCases The collection of use cases handling device token persistence.
 * @param pushService The collection of use cases handling push logic.
 * @param registryUseCases The registry use cases used to record request telemetry.
 */
fun Route.pushRoutes(
    permissionService: PermissionService,
    deviceTokenUseCases: DeviceTokenUseCases,
    pushService: PushService,
    registryUseCases: RegistryUseCases
) {
    authenticate {
        post(path = URL.DeviceTokens.path) {
            val userUuid = call.getUserUuid() ?: return@post
            val registration = call.receive<DeviceToken>() ?: return@post
            if (registration.token.isBlank() || registration.deviceUuid == null) {
                call.respondSafely(status = HttpStatusCode.BadRequest, message = "Invalid device token parameters")
                return@post
            }

            val success = deviceTokenUseCases.registerToken(
                userUuid = userUuid,
                registration = registration,
            )
            if (!success) {
                call.respondSafely(status = HttpStatusCode.InternalServerError, message = "Unable to register device token")
                return@post
            }

            call.respondSafely(status = HttpStatusCode.OK, message = "Device token registered")
            Telemetry.info(tag = TAG, message = "Registered device token for user $userUuid")
        }

        post(path = URL.Tickets.path) {
            val userUuid = call.getUserUuid() ?: return@post
            val ticket = pushService.generateTicket(userUuid = userUuid) ?: run {
                call.respondSafely(status = HttpStatusCode.InternalServerError, message = "Unable to generate ticket")
                return@post
            }

            call.respondSafely(status = HttpStatusCode.OK, message = ticket)
            Telemetry.info(tag = TAG, message = "Generated ticket for user $userUuid")
        }

        post(path = URL.Push.path) {
            if (!call.validatePermission(permissionService = permissionService, entityType = EntityType.NOTIFICATION, permission = Permission.WRITE)) return@post
            val pushPayload = call.receive<PushPayload>() ?: return@post
            pushService.sendNotification(pushPayload = pushPayload)
            call.respondSafely(status = HttpStatusCode.OK, message = "Push sent")
            Telemetry.info(tag = TAG, message = "Push sent")

            call.toRegistry(
                entityType = EntityType.NOTIFICATION,
                entityUuid = pushPayload.uuid,
                payload = encode(value = pushPayload)
            )?.let { registryUseCases.upsertRegistry(registry = it) }
        }
    }

    webSocket(path = URL.Push.path) {
        val queryMap = call.getQueryMap()
        val ticketUuid = queryMap[Query.TicketUuid.parameter]?.toUuid()
        val deviceUuid = queryMap[Query.DeviceUuid.parameter]?.toUuid()
        if (ticketUuid == null || deviceUuid == null) {
            Telemetry.error(tag = TAG, message = "WebSocket connection rejected: Missing parameters")
            close(reason = CloseReason(code = CloseReason.Codes.VIOLATED_POLICY, message = "Missing parameters"))
            return@webSocket
        }

        val userUuid = pushService.addConnection(ticketUuid = ticketUuid, deviceUuid = deviceUuid, session = this) ?: run {
            Telemetry.error(tag = TAG, message = "Invalid or expired WS ticket used from device $deviceUuid")
            close(reason = CloseReason(code = CloseReason.Codes.VIOLATED_POLICY, message = "Invalid ticket"))
            return@webSocket
        }

        Telemetry.info(tag = TAG, message = "User $userUuid connected device $deviceUuid to push stream")

        try {
            incoming.consumeEach {
                val uuid = (it as? Frame.Text)?.readText()?.toUuid() ?: return@consumeEach
                pushService.acknowledgePush(pushUuid = uuid)
            }
        } catch (exception: Exception) {
            Telemetry.error(tag = TAG, message = "WebSocket error for user $userUuid in device $deviceUuid: ${exception.localizedMessage}")
        } finally {
            pushService.removeConnection(userUuid = userUuid, deviceUuid = deviceUuid, session = this)
            Telemetry.info(tag = TAG, message = "Device $deviceUuid for user $userUuid disconnected")
        }
    }
}

private const val TAG = "PushRoute"