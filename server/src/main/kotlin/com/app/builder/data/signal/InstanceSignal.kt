package com.app.builder.data.signal

import kotlinx.coroutines.flow.Flow

/**
 * A lightweight publish/subscribe bus for signaling events across server instances.
 * Payloads are fire-and-forget wake-up messages, not durable data. Anything that must survive delivery belongs in the database.
 */
interface InstanceSignal {

    /**
     * Publishes a payload to every instance observing the channel, including this one.
     *
     * @param channel The channel to publish to.
     * @param payload The payload to deliver. Keep it small — implementations may cap its size.
     * @return `true` if the payload was published, `false` otherwise.
     */
    suspend fun notify(channel: String, payload: String): Boolean

    /**
     * Observes a channel, emitting every payload published to it by any instance.
     * The returned flow never completes normally and transparently survives transport failures.
     *
     * @param channel The channel to observe.
     * @return A flow of published payloads.
     */
    fun observe(channel: String): Flow<String>
}