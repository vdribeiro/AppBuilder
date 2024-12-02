package com.app.builder.data.http

import kotlin.coroutines.CoroutineContext
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.SendChannel
import io.ktor.server.application.ApplicationCall
import io.ktor.server.sse.ServerSSESession
import io.ktor.sse.ServerSentEvent
import io.ktor.websocket.CloseReason
import io.ktor.websocket.DefaultWebSocketSession
import io.ktor.websocket.Frame
import io.ktor.websocket.PINGER_DISABLED
import io.ktor.websocket.WebSocketExtension
import com.app.builder.core.flow.Dispatcher

/**
 * A fake [ServerSSESession] for testing purposes.
 *
 * @property fail Indicates whether the session should fail or not.
 */
class FakeSSESession(private val fail: Boolean = false): ServerSSESession {
    override val coroutineContext: CoroutineContext = SupervisorJob() + Dispatcher.IO
    override val call: ApplicationCall get() = error("Not implemented")
    /** Whether at least one event was sent through this session. */
    var sent = false
    /** The number of events sent through this session. */
    var sentCount = 0

    override suspend fun send(event: ServerSentEvent) {
        if (fail) throw RuntimeException("Session failed")
        sent = true
        sentCount++
    }

    override suspend fun send(data: String?, event: String?, id: String?, retry: Long?, comments: String?) {
        if (fail) throw RuntimeException("Session failed")
        sent = true
        sentCount++
    }

    override suspend fun close() {}
}

/**
 * A fake [DefaultWebSocketSession] for testing purposes.
 *
 * @property fail Indicates whether the session should fail or not.
 */
class FakeWebSocketSession(private val fail: Boolean = false): DefaultWebSocketSession {
    override val coroutineContext: CoroutineContext = SupervisorJob() + Dispatcher.IO
    override var masking: Boolean = false
    override var maxFrameSize: Long = Long.MAX_VALUE
    override val incoming: ReceiveChannel<Frame> = Channel()
    override val outgoing: SendChannel<Frame> = Channel()
    override val extensions: List<WebSocketExtension<*>> = emptyList()
    override var pingIntervalMillis: Long = PINGER_DISABLED
    override var timeoutMillis: Long = 1000L
    override val closeReason: Deferred<CloseReason?> = CompletableDeferred()
    /** Whether at least one frame was sent through this session. */
    var sent = false
    /** The number of frames sent through this session. */
    var sentCount = 0

    override fun start(negotiatedExtensions: List<WebSocketExtension<*>>) {}

    override suspend fun send(frame: Frame) {
        if (fail) throw RuntimeException("Session failed")
        sent = true
        sentCount++
    }

    override suspend fun flush() {}

    @Deprecated(message = "Overriding deprecated")
    override fun terminate() {
    }
}