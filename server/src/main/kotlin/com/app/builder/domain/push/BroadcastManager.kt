package com.app.builder.domain.push

import java.util.Collections
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import io.ktor.server.sse.ServerSSESession
import com.app.builder.core.config.ServerConfigs
import com.app.builder.core.config.ServerFlags
import com.app.builder.core.flow.Dispatcher
import com.app.builder.core.security.uuid
import com.app.builder.core.telemetry.Telemetry
import com.app.builder.data.serializer.encode
import com.app.builder.data.signal.InstanceSignal
import com.app.builder.domain.PushPayload

/**
 * Manages broadcast connections and fan-out delivery.
 *
 * @property instanceSignal Relays broadcast payloads to every server instance.
 * @property fcmService Sends payloads via Firebase Cloud Messaging.
 */
class BroadcastManager(
    private val instanceSignal: InstanceSignal,
    private val fcmService: FcmService,
): BroadcastService {

    /** Scope for the broadcast service. */
    private val scope = CoroutineScope(context = SupervisorJob())

    /** Guards concurrent start()/stop() transitions. */
    private val mutex = Mutex()

    /** Identifies this instance to prevent echoing broadcasts. */
    private val instanceId = uuid()

    /** Set of active SSE sessions. */
    private val connections = Collections.synchronizedSet<ServerSSESession>(LinkedHashSet())

    /** Tracks the signal observation delivering broadcasts to this instance's sessions. */
    private var listenJob: Job? = null

    override suspend fun stop() = withContext(context = Dispatcher.IO) {
        mutex.withLock {
            listenJob?.cancelAndJoin()
            listenJob = null
        }
    }

    override fun start() {
        if (!ServerFlags.flags.broadcast) return
        if (!mutex.tryLock()) return
        try {
            startListenJob()
        } finally {
            mutex.unlock()
        }
    }

    override fun addConnection(session: ServerSSESession): Boolean {
        val broadcastMaxConnections = ServerConfigs.configs.broadcastMaxConnections
        if (connections.size >= broadcastMaxConnections) {
            Telemetry.error(tag = TAG, message = "Broadcast connection rejected: limit reached (${connections.size}/$broadcastMaxConnections)")
            return false
        }
        return connections.add(element = session).also { Telemetry.info(tag = TAG, message = "Broadcast connection added. Active: ${connections.size}") }
    }

    override fun removeConnection(session: ServerSSESession): Boolean =
        connections.remove(element = session).also { Telemetry.info(tag = TAG, message = "Broadcast connection removed. Active: ${connections.size}") }

    override suspend fun broadcast(pushPayload: PushPayload) = withContext(context = Dispatchers.IO) {
        val data = encode(value = pushPayload) ?: return@withContext

        sendToConnections(data = data)
        if (!instanceSignal.notify(channel = CHANNEL, payload = "$instanceId$ENVELOPE_SEPARATOR$data")) {
            Telemetry.error(tag = TAG, message = "Unable to relay broadcast ${pushPayload.uuid} to other instances")
        }
        scope.launch(context = Dispatcher.IO) { fcmService.sendToBroadcastTopic(pushPayload = pushPayload) }
    }

    /**
     * Observes other instances.
     * If the job is already running, this is a no-op.
     */
    private fun startListenJob() {
        if (listenJob?.isActive != true) listenJob = scope.launch(context = Dispatcher.IO) {
            instanceSignal.observe(channel = CHANNEL).collect { envelope ->
                if (envelope.substringBefore(delimiter = ENVELOPE_SEPARATOR) == instanceId.toString()) return@collect
                sendToConnections(data = envelope.substringAfter(delimiter = ENVELOPE_SEPARATOR))
            }
        }
    }

    /**
     * Delivers a broadcast payload to this instance's SSE sessions, dropping sessions that repeatedly fail.
     *
     * @param data The encoded payload to deliver.
     */
    private fun sendToConnections(data: String) {
        connections.forEach { session ->
            scope.launch(context = Dispatcher.IO) {
                val delivered = session.isActive && sendWithRetry(
                    maxAttempts = ServerConfigs.configs.broadcastMaxAttempts,
                    retryDelay = ServerConfigs.configs.broadcastRetryDelay
                ) { session.send(data = data) }

                if (!delivered) removeConnection(session = session)
            }
        }
    }

    companion object {
        private const val TAG = "BroadcastManager"

        /** The signal channel relaying encoded broadcast payloads. */
        private const val CHANNEL = "broadcast"

        /** Separates the originating instance id from the payload in relayed broadcast envelopes. */
        private const val ENVELOPE_SEPARATOR = '|'
    }
}
