package com.app.builder.domain.push

import java.util.concurrent.ConcurrentHashMap
import kotlin.uuid.Uuid
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import io.ktor.websocket.DefaultWebSocketSession
import io.ktor.websocket.Frame
import com.app.builder.core.config.ServerConfigs
import com.app.builder.core.config.ServerFlags
import com.app.builder.core.flow.Dispatcher
import com.app.builder.core.security.uuid
import com.app.builder.core.telemetry.Telemetry
import com.app.builder.data.serializer.encode
import com.app.builder.domain.PushPayload
import com.app.builder.domain.usecase.connection.ConnectionUseCases
import com.app.builder.domain.usecase.devicetoken.DeviceTokenUseCases
import com.app.builder.domain.usecase.getOrNull
import com.app.builder.domain.usecase.notification.NotificationsUseCases
import com.app.builder.domain.usecase.ticket.TicketUseCases

/**
 * Manages push payloads and its single-use, short-lived tickets for connections, and allows registering an FCM token.
 * Saved notifications are sent once via Firebase Cloud Messaging by the instance that saved them, and via WebSocket by every instance holding a connection of their user.
 * Connection presence is persisted, so delivery decisions account for devices connected to any server instance, while the sockets themselves stay in this instance's memory.
 *
 * @property deviceTokenUseCases The collection of use cases for looking up a user's FCM device tokens.
 * @property ticketUseCases The collection of use cases handling connection ticket logic.
 * @property connectionUseCases The collection of use cases handling connection presence logic.
 * @property notificationsUseCases The collection of use cases handling push logic.
 * @property fcmService Sends payloads via Firebase Cloud Messaging.
 */
class PushManager(
    private val deviceTokenUseCases: DeviceTokenUseCases,
    private val ticketUseCases: TicketUseCases,
    private val connectionUseCases: ConnectionUseCases,
    private val notificationsUseCases: NotificationsUseCases,
    private val fcmService: FcmService
): PushService {

    /** Scope for the push service. */
    private val scope = CoroutineScope(context = SupervisorJob())

    /** Guards concurrent start()/stop() transitions. */
    private val mutex = Mutex()

    /** Identifies this server instance in the shared connection presence table. */
    private val instanceId = uuid()

    /** Maps a User to a map of Device UUID to WebSocket session */
    private val connections = ConcurrentHashMap<Uuid, ConcurrentHashMap<Uuid, DefaultWebSocketSession>>()

    /** Tracks active database observation jobs per user to prevent redundant flows. */
    private val observationJobs = ConcurrentHashMap<Uuid, Job>()

    /** Keeps this instance's presence rows alive. */
    private var heartbeatJob: Job? = null

    /** Tracks the observation of the push socket and push Firebase server feature flags. */
    private var flagJob: Job? = null

    /** Tracks the delivery of the notifications saved through this instance via Firebase Cloud Messaging. */
    private var savedJob: Job? = null

    /** Whether the heartbeat and the per-user observers are running. */
    @Volatile
    private var isRunning = false

    override suspend fun stop() = withContext(context = Dispatcher.IO) {
        mutex.withLock {
            flagJob?.cancelAndJoin()
            flagJob = null
            stopSavedJob()
            stopSocketJobs()
        }
    }

    override fun start() {
        if (!mutex.tryLock()) return
        try {
            if (flagJob?.isActive == true) return
            if (ServerFlags.value.pushSocket || ServerFlags.value.pushFirebase) startSavedJob()
            if (ServerFlags.value.pushSocket) startSocketJobs()
            flagJob = scope.launch {
                ServerFlags.flow
                    .map { (it.pushSocket || it.pushFirebase) to it.pushSocket }
                    .distinctUntilChanged()
                    .collect { (enabled, pushSocket) ->
                        mutex.withLock {
                            if (enabled) startSavedJob() else stopSavedJob()
                            if (pushSocket) startSocketJobs() else stopSocketJobs()
                        }
                    }
            }
        } finally {
            mutex.unlock()
        }
    }

    override suspend fun generateTicket(userUuid: Uuid): Uuid? =
        ticketUseCases.createTicket(userUuid = userUuid).getOrNull()

    override suspend fun addConnection(ticketUuid: Uuid, deviceUuid: Uuid, session: DefaultWebSocketSession): Uuid? {
        val userUuid = ticketUseCases.consumeTicket(ticketUuid = ticketUuid).getOrNull() ?: return null
        if (!connectionUseCases.addConnection(userUuid = userUuid, deviceUuid = deviceUuid, instanceId = instanceId)) return null

        connections.getOrPut(key = userUuid) { ConcurrentHashMap() }[deviceUuid] = session
        if (isRunning) startObservingForUser(userUuid = userUuid)
        return userUuid
    }

    override suspend fun removeConnection(userUuid: Uuid, deviceUuid: Uuid, session: DefaultWebSocketSession): Boolean {
        val userDevices = connections[userUuid] ?: return false
        if (!userDevices.remove(deviceUuid, session)) return false

        if (userDevices.isEmpty()) {
            connections.remove(userUuid)
            observationJobs.remove(userUuid)?.cancel()
        }
        connectionUseCases.removeConnection(userUuid = userUuid, deviceUuid = deviceUuid, instanceId = instanceId)
        return true
    }

    override suspend fun sendNotification(pushPayload: PushPayload) = withContext(context = Dispatcher.IO) {
        val encodedNotification = encode(value = pushPayload) ?: return@withContext
        val notificationUserUuid = pushPayload.userUuid
        if (notificationUserUuid == null) {
            Telemetry.error(tag = TAG, message = "No user uuid for notification ${pushPayload.uuid}. Only authenticated notifications are allowed")
            return@withContext
        }

        sendToWebSocketConnections(userUuid = notificationUserUuid, encodedNotification = encodedNotification)
        sendToFcmTokens(userUuid = notificationUserUuid, pushPayload = pushPayload)
    }

    override suspend fun acknowledgePush(pushUuid: Uuid): Boolean = withContext(context = Dispatcher.IO) {
        notificationsUseCases.deleteNotification(notificationUuid = pushUuid)
    }

    /**
     * Starts delivering the notifications saved through this instance via Firebase Cloud Messaging.
     * A notification is deleted once sent while the push socket server feature flag is off.
     * If the job is already running, this is a no-op.
     */
    private fun startSavedJob() {
        if (savedJob?.isActive != true) savedJob = scope.launch(context = Dispatcher.IO) {
            notificationsUseCases.observeSavedNotifications().collect { pushPayload ->
                val userUuid = pushPayload.userUuid ?: return@collect
                sendToFcmTokens(userUuid = userUuid, pushPayload = pushPayload)
                if (!ServerFlags.value.pushSocket) notificationsUseCases.deleteNotification(notificationUuid = pushPayload.uuid)
            }
        }
    }

    /** Stops delivering the notifications saved through this instance. */
    private suspend fun stopSavedJob() {
        savedJob?.cancelAndJoin()
        savedJob = null
    }

    /**
     * Starts the heartbeat and the observation for all currently connected users.
     * If they are already running, this is a no-op.
     */
    private fun startSocketJobs() {
        if (isRunning) return
        isRunning = true
        startHeartbeat()
        connections.keys.forEach { userUuid -> startObservingForUser(userUuid = userUuid) }
    }

    /** Stops the heartbeat and all database observers. */
    private suspend fun stopSocketJobs() {
        isRunning = false
        heartbeatJob?.cancelAndJoin()
        heartbeatJob = null
        observationJobs.values.forEach { it.cancelAndJoin() }
        observationJobs.clear()
    }

    /** Launches the periodic heartbeat keeping this instance's presence rows alive while it holds connections, so other instances treat its connections as live. */
    private fun startHeartbeat() {
        if (heartbeatJob?.isActive != true) heartbeatJob = scope.launch(context = Dispatcher.IO) {
            while (isActive) {
                if (connections.isNotEmpty()) connectionUseCases.touchConnections(instanceId = instanceId)
                delay(timeMillis = ServerConfigs.value.presenceHeartbeatInterval)
            }
        }
    }

    /** Launches an isolated coroutine to observe a specific user's notifications and dispatch them to the user's WebSocket connections. */
    private fun startObservingForUser(userUuid: Uuid) {
        observationJobs.computeIfAbsent(userUuid) {
            scope.launch(context = Dispatcher.IO) {
                notificationsUseCases.observeNotifications(userUuid = userUuid).collect { notifications ->
                    notifications.forEach { pushPayload ->
                        val encodedNotification = encode(value = pushPayload) ?: return@forEach
                        sendToWebSocketConnections(userUuid = userUuid, encodedNotification = encodedNotification)
                    }
                }
            }
        }
    }

    /**
     * Delivers to this user's devices via WebSocket connections.
     *
     * @param userUuid The user to deliver to.
     * @param encodedNotification The payload to deliver.
     */
    private fun sendToWebSocketConnections(userUuid: Uuid, encodedNotification: String) {
        val userDevices = connections[userUuid]
        if (userDevices.isNullOrEmpty()) return

        userDevices.forEach { (deviceUuid, session) ->
            scope.launch(context = Dispatcher.IO) {
                val delivered = session.isActive && sendWithRetry(
                    maxAttempts = ServerConfigs.value.pushMaxAttempts,
                    retryDelay = ServerConfigs.value.pushRetryDelay
                ) { session.send(frame = Frame.Text(text = encodedNotification)) }

                if (!delivered) removeConnection(userUuid = userUuid, deviceUuid = deviceUuid, session = session)
            }
        }
    }

    /**
     * Delivers to this user's devices via Firebase Cloud Messaging, skipping any device that already has a live WebSocket connection — on any server instance — to avoid delivering the same payload twice.
     * No device is skipped while the push socket server feature flag is off, and nothing is delivered while the push Firebase server feature flag is off.
     *
     * @param userUuid The user to deliver to.
     * @param pushPayload The payload to deliver.
     */
    private fun sendToFcmTokens(userUuid: Uuid, pushPayload: PushPayload) {
        if (!ServerFlags.value.pushFirebase) return
        scope.launch(context = Dispatcher.IO) {
            val connectedDeviceUuids = if (ServerFlags.value.pushSocket) connectionUseCases.getConnectedDeviceUuids(userUuid = userUuid) else emptyList()
            val registered = deviceTokenUseCases.getTokens(userUuid = userUuid).getOrNull() ?: return@launch
            val tokens = registered.filterNot { it.deviceUuid in connectedDeviceUuids }.map { it.token }
            if (tokens.isEmpty()) return@launch

            fcmService.sendToTokens(
                tokens = tokens,
                pushPayload = pushPayload
            ).forEach { token -> deviceTokenUseCases.removeToken(token = token) }
        }
    }

    companion object {
        private const val TAG = "PushManager"
    }
}
