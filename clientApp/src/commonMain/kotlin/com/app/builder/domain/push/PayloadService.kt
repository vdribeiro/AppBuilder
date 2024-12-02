package com.app.builder.domain.push

/** Payload service that listens for push payloads. */
interface PayloadService {

    /** Stops the service. */
    suspend fun stop()

    /**
     * Starts the service.
     * If the service is already running, this is a no-op.
     */
    fun start()
}