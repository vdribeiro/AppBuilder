package com.app.builder.domain.push

/** Push service that listens for user push payloads. */
interface PushService {

    /** Stops the push service, suspending until all jobs have fully canceled. */
    suspend fun stop()

    /**
     * Starts the push service by requesting a ticket and opening a WebSocket stream.
     * Reconnects automatically on failure utilizing an exponential backoff strategy.
     * If the service is already running, this is a no-op.
     */
    fun start()
}
