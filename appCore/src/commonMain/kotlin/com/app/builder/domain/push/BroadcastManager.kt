package com.app.builder.domain.push

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
import com.app.builder.core.config.ClientConfigs
import com.app.builder.core.config.ClientFlags
import com.app.builder.core.flow.Dispatcher
import com.app.builder.core.telemetry.Telemetry
import com.app.builder.data.http.HttpRequest
import com.app.builder.data.http.HttpResult
import com.app.builder.data.http.URL
import com.app.builder.data.http.getInternetAvailabilityFlow
import com.app.builder.data.http.post
import com.app.builder.data.http.sse
import com.app.builder.data.serializer.decode
import com.app.builder.domain.DeviceToken
import com.app.builder.domain.PushPayload

/**
 * Manager implementation for the SSE broadcast stream.
 * Maintains a persistent SSE connection and reconnects automatically on failure.
 *
 * @property httpClient The HTTP client.
 */
class BroadcastManager(
    private val httpClient: HttpClient,
): BroadcastService {

    /** An isolated scope for the manager's coroutine operations. */
    private val scope = CoroutineScope(context = SupervisorJob())

    /** Guards concurrent start()/stop() transitions. */
    private val mutex = Mutex()

    /** Tracks the current FCM token. */
    private var tokenJob: Job? = null

    /** Tracks the currently active broadcast loop. */
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
        if (!ClientFlags.flags.broadcast) return
        if (!mutex.tryLock()) return
        try {
            startSubscribeJob()
            startListenJob()
        } finally {
            mutex.unlock()
        }
    }

    /**
     * Subscribes to broadcast.
     * If the job is already running, this is a no-op.
     */
    private fun startSubscribeJob() {
        if (tokenJob?.isActive != true) tokenJob = scope.launch(context = Dispatcher.Default) {
            Telemetry.info(tag = TAG, message = "Subscribe to FCM broadcasts")
            PushProvider.token.filterNotNull().collectLatest {
                subscribeToBroadcast(token = it)
            }
        }
    }

    /**
     * Listens for broadcasts.
     * If the job is already running, this is a no-op.
     */
    private fun startListenJob() {
        if (listenJob?.isActive != true) listenJob = scope.launch(context = Dispatcher.IO) {
            Telemetry.info(tag = TAG, message = "Listen to broadcasts")
            getInternetAvailabilityFlow().collectLatest { online ->
                if (!online) return@collectLatest

                var retryDelay = ClientConfigs.configs.broadcastRetryDelay
                while (isActive) {
                    listen(
                        onConnected = { retryDelay = ClientConfigs.configs.broadcastRetryDelay }
                    ) { pushPayload ->
                        Telemetry.info(tag = TAG, message = "Received push payload: ${pushPayload.uuid}")
                        PushProvider.handlePushPayload(pushPayload = pushPayload)
                    }
                    delay(timeMillis = retryDelay)
                    retryDelay = (retryDelay * 2).coerceAtMost(maximumValue = ClientConfigs.configs.broadcastMaxRetryDelay)
                }
            }
        }
    }

    /**
     * Subscribes this device's FCM token to the broadcast topic, so it also receives broadcasts while backgrounded.
     *
     * @param token The FCM registration token.
     * @return `true` if the subscription request succeeded.
     */
    private suspend fun subscribeToBroadcast(token: String): Boolean = withContext(context = Dispatcher.IO) {
        when (val result = httpClient.post<DeviceToken, Unit>(
            request = HttpRequest(url = URL.BroadcastSubscribe),
            body = DeviceToken(token = token),
        )) {
            is HttpResult.Error -> {
                Telemetry.error(tag = TAG, message = "Unable to subscribe to broadcast", throwable = result.error)
                false
            }

            is HttpResult.Success -> {
                Telemetry.info(tag = TAG, message = "Subscribed to broadcast")
                true
            }
        }
    }

    /**
     * Establishes an SSE connection to the broadcast endpoint and suspends until disconnected.
     *
     * @param onConnected A callback invoked once the connection is established.
     * @param block A callback invoked for every incoming event.
     */
    private suspend fun listen(onConnected: suspend () -> Unit, block: suspend (PushPayload) -> Unit) = withContext(context = Dispatcher.IO) {
        httpClient.sse(
            request = HttpRequest(url = URL.Broadcast),
            onConnected = {
                Telemetry.info(tag = TAG, message = "Broadcast stream connected")
                onConnected()
            }
        ) { sse ->
            val payload = sse.data?.let { decode<PushPayload>(value = it) } ?: return@sse
            block(payload)
        }
        Unit
    }

    companion object {
        private const val TAG = "BroadcastManager"
    }
}