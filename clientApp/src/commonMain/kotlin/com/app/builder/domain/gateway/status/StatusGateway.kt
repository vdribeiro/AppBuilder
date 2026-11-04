package com.app.builder.domain.gateway.status

import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.transformLatest
import com.app.builder.core.config.ClientConfigs
import com.app.builder.core.config.ClientFlags
import com.app.builder.core.locale.now
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

    override fun observeSynced(): Flow<Boolean> =
        combine(flow = scheduler.observeActiveJobs(), flow2 = ClientFlags.flagsFlow) { jobs, flags ->
            if (flags.scheduler && flags.http) jobs else emptyList()
        }.transformLatest { jobs ->
            val oldest = jobs.minOfOrNull { it.utc }
            if (oldest == null) {
                emit(value = true)
                return@transformLatest
            }
            // The oldest job is not late yet, so report synced and wait until it would be
            val remaining = oldest + ClientConfigs.configs.schedulerSyncTolerance.milliseconds - now()
            if (remaining > Duration.ZERO) {
                emit(value = true)
                delay(duration = remaining)
            }
            emit(value = false)
        }.distinctUntilChanged()

    override fun observeClockTrusted(): Flow<Boolean> = clockService.trusted
}
