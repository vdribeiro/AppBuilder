package com.app.builder.domain.gateway.status

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import com.app.builder.domain.clock.ClockService
import com.app.builder.domain.scheduler.Scheduler

/**
 * Gateway implementation to follow the application status.
 *
 * @property scheduler The job scheduler for background tasks.
 * @property clockService The service managing the device's clock.
 */
class StatusGateway(
    private val scheduler: Scheduler,
    private val clockService: ClockService,
): StatusUseCases {

    override fun observeSynced(): Flow<Boolean> = scheduler.observeSyncing().map { !it }

    override fun observeClockTrusted(): Flow<Boolean> = clockService.trusted
}
