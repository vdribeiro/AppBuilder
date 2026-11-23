package com.app.builder.domain.route.provider

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import io.ktor.http.HttpStatusCode
import io.ktor.server.auth.authenticate
import io.ktor.server.plugins.ratelimit.RateLimitName
import io.ktor.server.plugins.ratelimit.rateLimit
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import io.ktor.server.sse.sse
import io.ktor.sse.ServerSentEvent
import com.app.builder.core.config.ServerConfigs
import com.app.builder.core.config.ServerFlags
import com.app.builder.core.telemetry.Telemetry
import com.app.builder.data.http.URL
import com.app.builder.data.http.plugin.RATE_LIMIT_BROADCAST
import com.app.builder.data.serializer.encode
import com.app.builder.domain.DeviceToken
import com.app.builder.domain.EntityType
import com.app.builder.domain.Permission
import com.app.builder.domain.PushPayload
import com.app.builder.domain.permission.PermissionService
import com.app.builder.domain.push.BroadcastService
import com.app.builder.domain.push.FcmService
import com.app.builder.domain.route.receive
import com.app.builder.domain.route.respondSafely
import com.app.builder.domain.route.toRegistry
import com.app.builder.domain.route.validatePermission
import com.app.builder.domain.usecase.registry.RegistryUseCases

/**
 * Configures the broadcast routes.
 * Publishing a broadcast is an authenticated endpoint, and requires write permission.
 *
 * @param permissionService The permission service.
 * @param broadcastService The service managing SSE connections and event dispatch.
 * @param registryUseCases The registry use cases used to record request telemetry.
 */
fun Route.broadcastRoutes(
    permissionService: PermissionService,
    broadcastService: BroadcastService,
    registryUseCases: RegistryUseCases
) {
    authenticate {
        post(path = URL.Broadcast.path) {
            if (!call.validatePermission(permissionService = permissionService, entityType = EntityType.NOTIFICATION, permission = Permission.WRITE)) return@post
            val pushPayload = call.receive<PushPayload>() ?: return@post
            broadcastService.broadcast(pushPayload = pushPayload)
            call.respondSafely(status = HttpStatusCode.OK, message = "Broadcast sent")
            Telemetry.info(tag = TAG, message = "Broadcast sent")

            call.toRegistry(
                entityType = EntityType.NOTIFICATION,
                entityUuid = pushPayload.uuid,
                payload = encode(value = pushPayload)
            )?.let { registryUseCases.upsertRegistry(registry = it) }
        }
    }
}

/**
 * Configures the broadcast stream route.
 * The SSE stream is public: any client may connect to receive notifications.
 * It is protected by per-IP rate limiting and by a global connection cap.
 *
 * @param broadcastService The service managing SSE connections and event dispatch.
 */
fun Route.broadcastStreamRoutes(broadcastService: BroadcastService) {
    rateLimit(configuration = RateLimitName(name = RATE_LIMIT_BROADCAST)) {
        sse(path = URL.Broadcast.path) {
            if (!broadcastService.addConnection(session = this)) return@sse
            val heartbeatJob = launch {
                while (isActive) {
                    send(event = ServerSentEvent(comments = "heartbeat"))
                    delay(timeMillis = ServerConfigs.value.broadcastPingInterval)
                }
            }
            try {
                ServerFlags.flow.first { !it.broadcastStream }
            } finally {
                heartbeatJob.cancel()
                broadcastService.removeConnection(session = this)
            }
        }
    }
}

/**
 * Configures the broadcast Firebase route.
 * The FCM topic subscription is public: any client may subscribe to receive notifications.
 * It is protected by per-IP rate limiting.
 *
 * @param fcmService The service managing FCM subscriptions.
 */
fun Route.broadcastFirebaseRoutes(fcmService: FcmService) {
    rateLimit(configuration = RateLimitName(name = RATE_LIMIT_BROADCAST)) {
        post(path = URL.BroadcastSubscribe.path) {
            val subscription = call.receive<DeviceToken>() ?: return@post
            fcmService.subscribeToBroadcastTopic(token = subscription.token)
            call.respondSafely(status = HttpStatusCode.OK, message = "Subscribed to broadcast")
        }
    }
}

private const val TAG = "BroadcastRoute"
