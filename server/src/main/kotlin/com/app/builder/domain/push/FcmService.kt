package com.app.builder.domain.push

import com.app.builder.domain.PushPayload

/** Sends push payloads via Firebase Cloud Messaging. */
interface FcmService {

    /**
     * Subscribes a device token to the broadcast topic, so it receives broadcast pushes.
     *
     * @param token The FCM registration token to subscribe.
     * @return Whether the subscription was successful.
     */
    suspend fun subscribeToBroadcastTopic(token: String): Boolean

    /**
     * Sends a payload to every device subscribed to the broadcast topic.
     *
     * @param pushPayload The payload to send.
     * @return Whether the payload was sent successfully.
     */
    suspend fun sendToBroadcastTopic(pushPayload: PushPayload): Boolean

    /**
     * Sends a payload to a specific set of FCM device tokens.
     *
     * @param tokens The FCM registration tokens to deliver to.
     * @param pushPayload The payload to send.
     * @return The subset of [tokens] that FCM reported as no longer registered, so callers can clean them up.
     */
    suspend fun sendToTokens(tokens: List<String>, pushPayload: PushPayload): List<String>
}
