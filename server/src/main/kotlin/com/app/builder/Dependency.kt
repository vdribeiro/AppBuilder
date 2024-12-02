package com.app.builder

import org.jetbrains.exposed.v1.r2dbc.R2dbcDatabase
import com.app.builder.data.signal.InstanceSignal
import com.app.builder.domain.permission.PermissionManager
import com.app.builder.domain.permission.PermissionService
import com.app.builder.domain.push.BroadcastManager
import com.app.builder.domain.push.BroadcastService
import com.app.builder.domain.push.FcmService
import com.app.builder.domain.push.PushManager
import com.app.builder.domain.push.PushService
import com.app.builder.domain.usecase.Gateways
import com.app.builder.domain.usecase.UseCases

/**
 * Dependency container for the application.
 *
 * @property database The reactive database instance.
 * @property instanceSignal The pub/sub bus.
 * @property fcmService The Firebase Cloud Messaging service.
 */
class Dependency(
    val database: R2dbcDatabase,
    val instanceSignal: InstanceSignal,
    val fcmService: FcmService,
) {

    /** The application use cases. */
    val useCases: UseCases = Gateways(database = database, instanceSignal = instanceSignal)

    /** The permission service. */
    val permissionService: PermissionService = PermissionManager(instanceSignal = instanceSignal)

    /** The broadcast service for SSE connections. */
    val broadcastService: BroadcastService = BroadcastManager(instanceSignal = instanceSignal, fcmService = fcmService)

    /** The push service for WebSocket connections. */
    val pushService: PushService = PushManager(
        deviceTokenUseCases = useCases.deviceTokenUseCases,
        ticketUseCases = useCases.ticketUseCases,
        connectionUseCases = useCases.connectionUseCases,
        notificationsUseCases = useCases.notificationsUseCases,
        fcmService = fcmService
    )
}