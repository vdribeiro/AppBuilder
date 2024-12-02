package com.app.builder.data.signal

import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.reactive.asFlow
import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.reactive.awaitFirstOrNull
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import com.app.builder.core.flow.Dispatcher
import com.app.builder.core.telemetry.Telemetry
import io.r2dbc.postgresql.PostgresqlConnectionFactory
import io.r2dbc.postgresql.api.PostgresqlConnection

/**
 * An [InstanceSignal] backed by Postgres LISTEN/NOTIFY, reaching every server instance connected to the same database.
 * Uses dedicated connections outside the regular pool: one for publishing and one per observed channel,
 * shared by every observer of that channel, no matter how many, each reconnecting automatically on failure.
 *
 * @property connectionFactory The factory for the dedicated signaling connections.
 */
class PostgresSignal(
    private val connectionFactory: PostgresqlConnectionFactory
): InstanceSignal {

    /** Hosts the shared LISTEN upstreams, one per channel, regardless of observer count. */
    private val scope = CoroutineScope(context = SupervisorJob() + Dispatcher.IO)

    /** One hot stream per channel, each backed by a single LISTEN connection held open while the channel has observers. */
    private val channels = ConcurrentHashMap<String, SharedFlow<String>>()

    /** Guards the publishing connection against concurrent statements. */
    private val notifyMutex = Mutex()

    /** The lazily created publishing connection. */
    private var notifyConnection: PostgresqlConnection? = null

    override suspend fun notify(channel: String, payload: String): Boolean = notifyMutex.withLock {
        runCatching {
            val connection = notifyConnection ?: connectionFactory.create().awaitFirst().also { notifyConnection = it }
            connection
                .createStatement("SELECT pg_notify($1, $2)")
                .bind("$1", channel)
                .bind("$2", payload)
                .execute()
                .asFlow()
                .collect { result -> result.rowsUpdated.awaitFirstOrNull() }
            true
        }.onFailure {
            Telemetry.error(tag = TAG, message = "Unable to notify channel $channel", throwable = it)
            notifyConnection?.closeSafely()
            notifyConnection = null
        }.getOrDefault(defaultValue = false)
    }

    override fun observe(channel: String): Flow<String> = channels.getOrPut(key = channel) {
        listen(channel = channel).shareIn(
            scope = scope,
            started = SharingStarted.WhileSubscribed(stopTimeoutMillis = STOP_TIMEOUT),
            replay = 0
        )
    }

    /** Closes the connection. */
    private suspend fun PostgresqlConnection.closeSafely() {
        runCatching {
            close().awaitFirstOrNull()
        }.onFailure {
            Telemetry.error(tag = TAG, message = "Unable to close notify connection", throwable = it)
        }
    }

    /**
     * Maintains the single LISTEN connection for a channel, emitting every received payload and reconnecting on failure.
     *
     * @param channel The channel to listen to.
     * @return A flow of received payloads.
     */
    private fun listen(channel: String): Flow<String> = flow {
        while (currentCoroutineContext().isActive) {
            var connection: PostgresqlConnection? = null
            try {
                connection = connectionFactory.create().awaitFirst()
                connection
                    .createStatement("""LISTEN "$channel"""")
                    .execute()
                    .asFlow()
                    .collect { result -> result.rowsUpdated.awaitFirstOrNull() }
                connection.notifications.asFlow().collect { notification ->
                    if (notification.name == channel) emit(value = notification.parameter.orEmpty())
                }
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (exception: Exception) {
                Telemetry.error(tag = TAG, message = "Lost LISTEN connection for channel $channel, reconnecting", throwable = exception)
            } finally {
                withContext(context = NonCancellable) { connection?.closeSafely() }
            }
            delay(timeMillis = RECONNECT_DELAY)
        }
    }

    companion object {
        private const val TAG = "PostgresSignal"

        /** Delay before re-establishing a lost LISTEN connection, in milliseconds. */
        private const val RECONNECT_DELAY = 1_000L

        /** How long a channel's LISTEN connection is kept open after its last observer leaves, avoiding churn when observers come and go in quick succession. */
        private const val STOP_TIMEOUT = 5_000L
    }
}
