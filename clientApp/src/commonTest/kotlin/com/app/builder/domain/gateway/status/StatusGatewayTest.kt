package com.app.builder.domain.gateway.status

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.milliseconds
import kotlin.uuid.Uuid
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import com.app.builder.core.config.ClientConfigs
import com.app.builder.core.config.ClientFlags
import com.app.builder.domain.EntityType
import com.app.builder.domain.clock.ClockService
import com.app.builder.domain.scheduler.Job
import com.app.builder.domain.scheduler.Scheduler
import com.app.builder.test.TestCase

class StatusGatewayTest: TestCase() {

    /** [Scheduler] fixture exposing a settable list of active jobs. */
    private class FakeScheduler: Scheduler {
        val activeJobs = MutableStateFlow(value = emptyList<Job>())
        override suspend fun stop() {}
        override fun start() {}
        override suspend fun queue(job: Job) {}
        override fun observeActiveJobs(): Flow<List<Job>> = activeJobs
    }

    /** [ClockService] fixture that reports a trusted clock. */
    private class FakeClockService: ClockService {
        override val trusted: StateFlow<Boolean> = MutableStateFlow(value = true)
        override fun start() {}
        override suspend fun stop() {}
    }

    /** Verifies that an active job only reports not synced once it has been active for longer than the tolerance, and synced again once it is gone. */
    @Test
    fun notSyncedAfterTolerance() = runUnitTest {
        val scheduler = FakeScheduler()
        val gateway = StatusGateway(scheduler = scheduler, clockService = FakeClockService())
        val tolerance = ClientConfigs.configs.schedulerSyncTolerance
        val values = mutableListOf<Boolean>()
        backgroundScope.launch { gateway.observeSynced().collect { values.add(it) } }
        testScheduler.runCurrent()
        assertEquals(expected = listOf(true), actual = values)

        scheduler.activeJobs.value = listOf(Job(userUuid = Uuid.NIL, entityType = EntityType.CLIENT_FLAG, type = Job.Type.GET))
        testScheduler.advanceTimeBy(delayTime = (tolerance - 1).milliseconds)
        assertEquals(expected = listOf(true), actual = values)

        testScheduler.advanceTimeBy(delayTime = 2.milliseconds)
        assertEquals(expected = listOf(true, false), actual = values)

        scheduler.activeJobs.value = emptyList()
        testScheduler.runCurrent()
        assertEquals(expected = listOf(true, false, true), actual = values)
    }

    /** Verifies that a job already older than the tolerance reports not synced right away, without a synced value first. */
    @Test
    fun staleJobReportsNotSyncedImmediately() = runUnitTest {
        val scheduler = FakeScheduler()
        val gateway = StatusGateway(scheduler = scheduler, clockService = FakeClockService())
        val job = Job(userUuid = Uuid.NIL, entityType = EntityType.CLIENT_FLAG, type = Job.Type.GET)
        testScheduler.advanceTimeBy(delayTime = (ClientConfigs.configs.schedulerSyncTolerance + 1).milliseconds)
        scheduler.activeJobs.value = listOf(job.copy(utc = job.utc - (ClientConfigs.configs.schedulerSyncTolerance + 1).milliseconds))

        val values = mutableListOf<Boolean>()
        backgroundScope.launch { gateway.observeSynced().collect { values.add(it) } }
        testScheduler.runCurrent()
        assertEquals(expected = listOf(false), actual = values)
    }

    /** Verifies that turning off the scheduler or networking reports synced regardless of late jobs. */
    @Test
    fun syncedWhileSchedulerOrHttpDisabled() = runUnitTest {
        val scheduler = FakeScheduler()
        val gateway = StatusGateway(scheduler = scheduler, clockService = FakeClockService())
        val values = mutableListOf<Boolean>()
        backgroundScope.launch { gateway.observeSynced().collect { values.add(it) } }
        scheduler.activeJobs.value = listOf(Job(userUuid = Uuid.NIL, entityType = EntityType.CLIENT_FLAG, type = Job.Type.GET))
        testScheduler.advanceTimeBy(delayTime = (ClientConfigs.configs.schedulerSyncTolerance + 1).milliseconds)
        assertEquals(expected = false, actual = values.last())

        ClientFlags.set { it.copy(http = false) }
        testScheduler.runCurrent()
        assertEquals(expected = true, actual = values.last())

        ClientFlags.set { it.copy(http = true, scheduler = false) }
        testScheduler.runCurrent()
        assertEquals(expected = true, actual = values.last())

        ClientFlags.set { it.copy(scheduler = true) }
        testScheduler.runCurrent()
        assertEquals(expected = false, actual = values.last())
    }
}
