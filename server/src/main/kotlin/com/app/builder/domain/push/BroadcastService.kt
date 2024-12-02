package com.app.builder.domain.push

import io.ktor.server.sse.ServerSSESession
import com.app.builder.domain.PushPayload

/** Manages the lifecycle of SSE broadcast connections and event dispatch. */
interface BroadcastService {

    /** Stops listening for broadcasts. */
    suspend fun stop()

    /** Starts listening for broadcasts published by any server instance, delivering them to this instance's SSE sessions. */
    fun start()

    /**
     * Registers an SSE session as an active broadcast recipient.
     *
     * @param session The connected [ServerSSESession].
     * @return `true` if the connection was successfully registered, `false` otherwise.
     */
    fun addConnection(session: ServerSSESession): Boolean

    /**
     * Removes a disconnected SSE session from the active recipients.
     *
     * @param session The disconnected [ServerSSESession].
     * @return `true` if the connection was successfully removed, `false` otherwise.
     */
    fun removeConnection(session: ServerSSESession): Boolean

    /**
     * Sends a [PushPayload] to the FCM broadcast topic and the SSE sessions of every server instance.
     *
     * @param pushPayload The push to broadcast.
     */
    suspend fun broadcast(pushPayload: PushPayload)
}
