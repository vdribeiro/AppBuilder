package com.app.builder.domain.push

import kotlin.uuid.Uuid
import io.ktor.websocket.DefaultWebSocketSession
import com.app.builder.domain.PushPayload

/** Manages push payloads and its single-use, short-lived tickets for connections. */
interface PushService {

    /** Stops the global push service and suspends all database observers. */
    suspend fun stop()

    /** Starts the global push service. Resumes observation for all currently connected users. */
    fun start()

    /**
     * Generates a single-use ticket for a user that expires automatically if the client fails to connect in time.
     *
     * @param userUuid The UUID of the user requesting the ticket.
     * @return The single-use ticket, or null if the ticket could not be created.
     */
    suspend fun generateTicket(userUuid: Uuid): Uuid?

    /**
     * Adds a new active connection for a specific user on a specific device.
     * Automatically syncs any pending push payloads for this user.
     *
     * @param ticketUuid The ticket provided by the client.
     * @param deviceUuid The unique identifier of the device.
     * @param session The active WebSocket session.
     * @return The User UUID associated with the ticket, or null if an error occurs or there in no matching user.
     */
    suspend fun addConnection(ticketUuid: Uuid, deviceUuid: Uuid, session: DefaultWebSocketSession): Uuid?

    /**
     * Removes an active connection if [session] still owns the device's connection entry.
     * A stale removal from an already-replaced connection never evicts the session that replaced it.
     *
     * @param userUuid The unique identifier of the user.
     * @param deviceUuid The unique identifier of the device.
     * @param session The WebSocket session the removal is for.
     * @return `true` if the connection was removed successfully, `false` otherwise.
     */
    suspend fun removeConnection(userUuid: Uuid, deviceUuid: Uuid, session: DefaultWebSocketSession): Boolean

    /**
     * Sends a notification to the user's devices, via WebSocket for devices with a live connection and via Firebase Cloud Messaging for the rest.
     *
     * @param pushPayload The notification to send.
     */
    suspend fun sendNotification(pushPayload: PushPayload)

    /**
     * Acknowledges that a push was received by a user.
     *
     * @param pushUuid The unique identifier of the push payload.
     * @return `true` if the push was acknowledged successfully, `false` otherwise.
     */
    suspend fun acknowledgePush(pushUuid: Uuid): Boolean
}