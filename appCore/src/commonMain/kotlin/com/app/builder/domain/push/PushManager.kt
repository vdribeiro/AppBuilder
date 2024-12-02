package com.app.builder.domain.push

import kotlin.uuid.Uuid
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import io.ktor.client.HttpClient
import io.ktor.websocket.Frame
import io.ktor.websocket.readText
import com.app.builder.core.config.ClientConfigs
import com.app.builder.core.config.ClientFlags
import com.app.builder.core.flow.Dispatcher
import com.app.builder.core.telemetry.Telemetry
import com.app.builder.data.device.getDeviceUuid
import com.app.builder.data.http.HttpRequest
import com.app.builder.data.http.HttpResult
import com.app.builder.data.http.Query
import com.app.builder.data.http.URL
import com.app.builder.data.http.getInternetAvailabilityFlow
import com.app.builder.data.http.post
import com.app.builder.data.http.webSocket
import com.app.builder.data.serializer.decode
import com.app.builder.domain.DeviceToken
import com.app.builder.domain.PushPayload
import com.app.builder.domain.User

/**
 * Manager implementation for push workflows.
 * Manages the lifecycle of a persistent WebSocket connection to receive user push payloads.
 *
 * @property user The current user.
 * @property httpClient The HTTP client used for network operations.
 */
class PushManager(
    private val user: User,
    private val httpClient: HttpClient,
): PushService {

    /** An isolated scope for the manager's coroutine operations. */
    private val scope = CoroutineScope(context = SupervisorJob())

    /** Guards concurrent start()/stop() transitions. */
    private val mutex = Mutex()

    /** Tracks the current FCM token. */
    private var tokenJob: Job? = null

    /** Tracks the currently active push loop. */
    private var listenJob: Job? = null

    override suspend fun stop() = withContext(context = Dispatcher.IO) {
        mutex.withLock {
            tokenJob?.cancelAndJoin()
            tokenJob = null
            listenJob?.cancelAndJoin()
            listenJob = null
        }
    }

    override fun start() {
        if (!ClientFlags.flags.push) return
        if (!mutex.tryLock()) return
        try {
            startTokenJob()
            startListenJob()
        } finally {
            mutex.unlock()
        }
    }

    /**
     * Observes and registers push token.
     * If the job is already running, this is a no-op.
     */
    private fun startTokenJob() {
        if (tokenJob?.isActive != true) tokenJob = scope.launch(context = Dispatcher.Default) {
            Telemetry.info(tag = TAG, message = "Registering token for push notifications")
            PushProvider.token.filterNotNull().collectLatest {
                registerDeviceToken(token = it)
            }
        }
    }

    /**
     * Listens for pushes.
     * If the job is already running, this is a no-op.
     */
    private fun startListenJob() {
        if (listenJob?.isActive != true) listenJob = scope.launch(context = Dispatcher.IO) {
            Telemetry.info(tag = TAG, message = "Listen to push notifications")
            getInternetAvailabilityFlow().collectLatest { online ->
                if (!online) return@collectLatest

                var retryDelay = ClientConfigs.configs.pushRetryDelay
                while (isActive) {
                    val ticketUuid = requestTicket()
                    if (ticketUuid != null) {
                        listen(
                            ticketUuid = ticketUuid,
                            onConnected = { retryDelay = ClientConfigs.configs.pushRetryDelay }
                        ) { userUuid, pushPayload ->
                            Telemetry.info(tag = TAG, message = "Received push payload: ${pushPayload.uuid}")
                            if (pushPayload.userUuid != userUuid) {
                                Telemetry.error(tag = TAG, message = "Push payload is for a different user ${pushPayload.userUuid}")
                                return@listen null
                            }
                            PushProvider.handlePushPayload(pushPayload = pushPayload)
                            pushPayload.uuid.toString()
                        }
                    }
                    delay(timeMillis = retryDelay)
                    retryDelay = (retryDelay * 2).coerceAtMost(maximumValue = ClientConfigs.configs.pushMaxRetryDelay)
                }
            }
        }
    }

    /**
     * Registers this device's FCM token with the server, so it can be delivered to directly via Firebase Cloud Messaging instead of (or in addition to) the WebSocket push stream.
     *
     * @param token The FCM registration token.
     * @return `true` if the token was registered successfully, `false` otherwise.
     */
    private suspend fun registerDeviceToken(token: String): Boolean = withContext(context = Dispatcher.IO) {
        val registration = DeviceToken(
            deviceUuid = getDeviceUuid(),
            token = token,
        )

        when (val result = httpClient.post<DeviceToken, Unit>(
            request = HttpRequest(url = URL.DeviceTokens),
            body = registration,
        )) {
            is HttpResult.Error -> {
                Telemetry.error(tag = TAG, message = "Unable to register device token", throwable = result.error)
                false
            }

            is HttpResult.Success -> {
                Telemetry.info(tag = TAG, message = "Successfully registered device token")
                true
            }
        }
    }

    /**
     * Requests a ticket for a user.
     *
     * @return The ticket UUID.
     */
    private suspend fun requestTicket(): Uuid? = withContext(context = Dispatcher.IO) {
        when (val result = httpClient.post<Unit, Uuid>(
            request = HttpRequest(url = URL.Tickets)
        )) {
            is HttpResult.Error -> {
                Telemetry.error(tag = TAG, message = "Unable to request a ticket", throwable = result.error)
                return@withContext null
            }

            is HttpResult.Success -> {
                Telemetry.info(tag = TAG, message = "Successfully requested a ticket")
                return@withContext result.data
            }
        }
    }

    /**
     * Listens for the push stream.
     *
     * @param ticketUuid The ticket UUID.
     * @param onConnected A callback function invoked after a successful connection.
     * @param block A callback invoked for every incoming event that returns the acknowledgement payload to the server.
     */
    private suspend fun listen(ticketUuid: Uuid, onConnected: suspend () -> Unit, block: suspend (Uuid, PushPayload) -> String?) = withContext(context = Dispatcher.IO) {
        val queryMap: Map<Query, String> = buildMap {
            put(key = Query.TicketUuid, value = ticketUuid.toString())
            put(key = Query.DeviceUuid, value = getDeviceUuid().toString())
        }
        httpClient.webSocket<Frame.Text>(
            request = HttpRequest(
                url = URL.Push,
                queryMap = queryMap,
            ),
            onConnected = {
                Telemetry.info(tag = TAG, message = "Push Stream connected")
                onConnected()
            }
        ) { frame ->
            val payload = decode<PushPayload>(value = frame.readText()) ?: return@webSocket
            block(user.uuid, payload)?.let { send(frame = Frame.Text(text = it)) }
        }
        Unit
    }

    companion object {
        private const val TAG = "PushManager"
    }
}