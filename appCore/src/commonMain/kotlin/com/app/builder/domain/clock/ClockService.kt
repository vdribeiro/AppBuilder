package com.app.builder.domain.clock

import kotlinx.coroutines.flow.StateFlow

/** Clock service that tracks whether the device clock can be trusted. */
interface ClockService {

    /** Whether the device clock can be trusted. */
    val trusted: StateFlow<Boolean>

    /** Stops the clock service, suspending until its background loops have fully canceled. */
    suspend fun stop()

    /**
     * Starts the clock service.
     * If the service is already running, this is a no-op.
     */
    fun start()
}