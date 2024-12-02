package com.app.builder.domain.push

/** Broadcast service that listens for server-sent push payloads. */
interface BroadcastService {

    /** Stops the broadcast service, suspending until all jobs have fully canceled. */
    suspend fun stop()

    /**
     * Starts the broadcast service by opening an SSE stream.
     * Reconnects automatically on failure utilizing an exponential backoff strategy.
     * If the service is already running, this is a no-op.
     */
    fun start()
}