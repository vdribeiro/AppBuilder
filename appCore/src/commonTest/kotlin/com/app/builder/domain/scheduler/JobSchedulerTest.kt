package com.app.builder.domain.scheduler

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.uuid.Uuid
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import com.app.builder.core.config.ClientConfigs
import com.app.builder.core.config.ClientFlags
import com.app.builder.core.security.uuid
import com.app.builder.data.http.plugin.InternetDisabledException
import com.app.builder.domain.EntityType
import com.app.builder.domain.clock.ClockService
import com.app.builder.test.TestCase

class JobSchedulerTest: TestCase() {

    /** [JobFactory] fixture that resolves every job to a fixed result and records resolved jobs. */
    private class FakeJobFactory(private val result: JobResult = JobResult.Success): JobFactory {
        /** Jobs that have been resolved by this factory, in resolution order. */
        val resolvedJobs = mutableListOf<Job>()
        override fun resolve(job: Job): suspend () -> JobResult = {
            resolvedJobs.add(element = job)
            result
        }
    }

    /** [JobFactory] fixture that throws a given throwable for a number of attempts before succeeding. */
    private class ThrowingJobFactory(private val throwable: Throwable, private val failuresBeforeSuccess: Int = Int.MAX_VALUE): JobFactory {
        /** Number of resolution attempts made so far. */
        private var attempts = 0
        override fun resolve(job: Job): suspend () -> JobResult = {
            attempts++
            if (attempts <= failuresBeforeSuccess) throw throwable else JobResult.Success
        }
    }

    /** [JobFactory] fixture that returns results from a fixed sequence, repeating the last one. */
    private class SequencedJobFactory(private val results: List<JobResult>): JobFactory {
        /** Position of the next result to return from [results]. */
        private var index = 0
        override fun resolve(job: Job): suspend () -> JobResult = {
            val result = results.getOrElse(index = index) { results.last() }
            index++
            result
        }
    }

    /** [JobStore] fixture whose every operation throws, for verifying failures are swallowed. */
    private class ThrowingJobStore: JobStore {
        override fun observePendingJobs(): Flow<List<Job>> = flow { throw IllegalStateException("boom") }
        override suspend fun upsertJob(job: Job): Boolean = throw IllegalStateException("boom")
        override suspend fun cancelPendingJobs(job: Job): Boolean = throw IllegalStateException("boom")
        override suspend fun getDuplicateJobsCount(job: Job): Long = throw IllegalStateException("boom")
        override suspend fun resetZombieJobs(): Long = throw IllegalStateException("boom")
        override suspend fun updateJobStateAndAttempt(job: Job): Boolean = throw IllegalStateException("boom")
    }

    /** [ClockService] fixture that reports a fixed trusted state. */
    private class FakeClockService(trusted: Boolean = true): ClockService {
        override val trusted = MutableStateFlow(value = trusted)
        override fun start() {}
        override suspend fun stop() {}
    }

    /** Verifies queuing a job with the append conflict policy keeps both jobs pending. */
    @Test
    fun queueWithAppendPolicy() = runUnitTest {
        val jobStore = LocalJobStore()
        val scheduler = JobScheduler(jobStore = jobStore, jobFactory = FakeJobFactory(), clockService = FakeClockService())

        val job = Job(userUuid = Uuid.NIL, entityType = EntityType.TRANSLATION, type = Job.Type.POST, conflictPolicy = Job.ConflictPolicy.APPEND)
        scheduler.queue(job = job)
        scheduler.queue(job = job.copy(uuid = uuid()))

        assertEquals(expected = 2, actual = jobStore.jobs.value.count { it.entityType == EntityType.TRANSLATION && it.state == Job.State.PENDING })
    }

    /** Verifies queuing a job with the ignore conflict policy keeps only one job pending. */
    @Test
    fun queueWithIgnorePolicy() = runUnitTest {
        val jobStore = LocalJobStore()
        val scheduler = JobScheduler(jobStore = jobStore, jobFactory = FakeJobFactory(), clockService = FakeClockService())

        val job = Job(userUuid = Uuid.NIL, entityType = EntityType.CLIENT_FLAG, type = Job.Type.GET)
        scheduler.queue(job = job)
        scheduler.queue(job = job.copy(uuid = uuid()))

        assertEquals(expected = 1, actual = jobStore.jobs.value.count { it.entityType == EntityType.CLIENT_FLAG && it.state == Job.State.PENDING })
    }

    /** Verifies queuing a job with the replace conflict policy cancels the previous pending job. */
    @Test
    fun queueWithReplacePolicy() = runUnitTest {
        val jobStore = LocalJobStore()
        val scheduler = JobScheduler(jobStore = jobStore, jobFactory = FakeJobFactory(), clockService = FakeClockService())

        val job = Job(userUuid = Uuid.NIL, entityType = EntityType.TRANSLATION, type = Job.Type.POST, conflictPolicy = Job.ConflictPolicy.REPLACE)
        scheduler.queue(job = job)
        scheduler.queue(job = job.copy(uuid = uuid()))

        assertEquals(expected = 1, actual = jobStore.jobs.value.count { it.entityType == EntityType.TRANSLATION && it.state == Job.State.PENDING })
        assertEquals(expected = 1, actual = jobStore.jobs.value.count { it.entityType == EntityType.TRANSLATION && it.state == Job.State.CANCELED })
    }

    /** Verifies a job queued while the scheduler is disabled is resolved immediately. */
    @Test
    fun queueWhenSchedulerDisabled() = runUnitTest {
        ClientFlags.set { it.copy(scheduler = false) }
        val jobFactory = FakeJobFactory()
        val scheduler = JobScheduler(jobStore = LocalJobStore(), jobFactory = jobFactory, clockService = FakeClockService())

        val job = Job(userUuid = Uuid.NIL, entityType = EntityType.CLIENT_FLAG, type = Job.Type.GET)
        scheduler.queue(job = job)

        assertEquals(expected = listOf(job), actual = jobFactory.resolvedJobs)
    }

    /** Verifies starting the scheduler executes pending jobs when online and the clock is trusted. */
    @Test
    fun startExecutesPendingJobsWhenOnlineAndTrusted() = runUnitTest {
        val jobStore = LocalJobStore()
        val jobFactory = FakeJobFactory()
        val scheduler = JobScheduler(jobStore = jobStore, jobFactory = jobFactory, clockService = FakeClockService())
        val job = Job(userUuid = Uuid.NIL, entityType = EntityType.CLIENT_FLAG, type = Job.Type.GET)

        scheduler.queue(job = job)
        scheduler.start()
        scheduler.stop()

        assertEquals(expected = Job.State.COMPLETE, actual = jobStore.jobs.value.first { it.uuid == job.uuid }.state)
    }

    /** Verifies starting the scheduler does not execute jobs while the clock is untrusted. */
    @Test
    fun startDoesNotExecuteJobsWhenClockIsUntrusted() = runUnitTest {
        val jobStore = LocalJobStore()
        val jobFactory = FakeJobFactory()
        val scheduler = JobScheduler(jobStore = jobStore, jobFactory = jobFactory, clockService = FakeClockService(trusted = false))
        val job = Job(userUuid = Uuid.NIL, entityType = EntityType.CLIENT_FLAG, type = Job.Type.GET)

        scheduler.queue(job = job)
        scheduler.start()
        scheduler.stop()

        assertEquals(expected = Job.State.PENDING, actual = jobStore.jobs.value.first { it.uuid == job.uuid }.state)
    }

    /** Verifies entity-targeted jobs execute before a global job for the same entity type. */
    @Test
    fun targetedJobsExecuteBeforeGlobalJobForSameEntity() = runUnitTest {
        val jobStore = LocalJobStore()
        val jobFactory = FakeJobFactory()
        val scheduler = JobScheduler(jobStore = jobStore, jobFactory = jobFactory, clockService = FakeClockService())
        val userUuid = uuid()
        val targetedJob = Job(userUuid = userUuid, entityUuid = uuid(), entityType = EntityType.TASK, type = Job.Type.POST)
        val globalJob = Job(userUuid = userUuid, entityType = EntityType.TASK, type = Job.Type.GET)

        scheduler.queue(job = globalJob)
        scheduler.queue(job = targetedJob)
        scheduler.start()
        scheduler.stop()

        assertEquals(expected = listOf(targetedJob.uuid, globalJob.uuid), actual = jobFactory.resolvedJobs.map { it.uuid })
        assertEquals(expected = Job.State.COMPLETE, actual = jobStore.jobs.value.first { it.uuid == targetedJob.uuid }.state)
        assertEquals(expected = Job.State.COMPLETE, actual = jobStore.jobs.value.first { it.uuid == globalJob.uuid }.state)
    }

    /** Verifies a retry job result reschedules the job until it eventually succeeds. */
    @Test
    fun retryResultReschedulesJobUntilItSucceeds() = runUnitTest {
        val jobStore = LocalJobStore()
        val jobFactory = SequencedJobFactory(results = listOf(JobResult.Retry, JobResult.Success))
        val scheduler = JobScheduler(jobStore = jobStore, jobFactory = jobFactory, clockService = FakeClockService())
        val job = Job(userUuid = Uuid.NIL, entityType = EntityType.CLIENT_FLAG, type = Job.Type.GET)

        scheduler.queue(job = job)
        scheduler.start()
        scheduler.stop()

        val saved = jobStore.jobs.value.first { it.uuid == job.uuid }
        assertEquals(expected = Job.State.COMPLETE, actual = saved.state)
        assertEquals(expected = 1, actual = saved.attempt)
    }

    /** Verifies a retry job result fails the job once the max attempt count is exceeded. */
    @Test
    fun retryResultFailsJobWhenMaxAttemptsExceeded() = runUnitTest {
        val jobStore = LocalJobStore()
        val scheduler = JobScheduler(jobStore = jobStore, jobFactory = FakeJobFactory(result = JobResult.Retry), clockService = FakeClockService())
        val job = Job(userUuid = Uuid.NIL, entityType = EntityType.CLIENT_FLAG, type = Job.Type.GET, attempt = ClientConfigs.configs.schedulerMaxAttempts)

        scheduler.queue(job = job)
        scheduler.start()
        scheduler.stop()

        assertEquals(expected = Job.State.FAILED, actual = jobStore.jobs.value.first { it.uuid == job.uuid }.state)
    }

    /** Verifies an error job result fails the job. */
    @Test
    fun errorResultFailsJob() = runUnitTest {
        val jobStore = LocalJobStore()
        val jobFactory = FakeJobFactory(result = JobResult.Error(error = IllegalStateException("boom")))
        val scheduler = JobScheduler(jobStore = jobStore, jobFactory = jobFactory, clockService = FakeClockService())
        val job = Job(userUuid = Uuid.NIL, entityType = EntityType.CLIENT_FLAG, type = Job.Type.GET)

        scheduler.queue(job = job)
        scheduler.start()
        scheduler.stop()

        assertEquals(expected = Job.State.FAILED, actual = jobStore.jobs.value.first { it.uuid == job.uuid }.state)
    }

    /** Verifies a job factory throwing an internet-disabled exception reschedules the job until it succeeds. */
    @Test
    fun jobFactoryThrowingInternetDisabledExceptionReschedulesJobUntilItSucceeds() = runUnitTest {
        val jobStore = LocalJobStore()
        val jobFactory = ThrowingJobFactory(throwable = InternetDisabledException(), failuresBeforeSuccess = 1)
        val scheduler = JobScheduler(jobStore = jobStore, jobFactory = jobFactory, clockService = FakeClockService())
        val job = Job(userUuid = Uuid.NIL, entityType = EntityType.CLIENT_FLAG, type = Job.Type.GET)

        scheduler.queue(job = job)
        scheduler.start()
        scheduler.stop()

        val saved = jobStore.jobs.value.first { it.uuid == job.uuid }
        assertEquals(expected = Job.State.COMPLETE, actual = saved.state)
        assertEquals(expected = 1, actual = saved.attempt)
    }

    /** Verifies a job factory throwing a generic exception fails the job. */
    @Test
    fun jobFactoryThrowingGenericExceptionFailsJob() = runUnitTest {
        val jobStore = LocalJobStore()
        val scheduler = JobScheduler(jobStore = jobStore, jobFactory = ThrowingJobFactory(throwable = IllegalStateException("boom")), clockService = FakeClockService())
        val job = Job(userUuid = Uuid.NIL, entityType = EntityType.CLIENT_FLAG, type = Job.Type.GET)

        scheduler.queue(job = job)
        scheduler.start()
        scheduler.stop()

        assertEquals(expected = Job.State.FAILED, actual = jobStore.jobs.value.first { it.uuid == job.uuid }.state)
    }

    /** Verifies job store failures during queuing are swallowed regardless of conflict policy. */
    @Test
    fun queueSwallowsJobStoreFailures() = runUnitTest {
        val scheduler = JobScheduler(jobStore = ThrowingJobStore(), jobFactory = FakeJobFactory(), clockService = FakeClockService())
        val job = Job(userUuid = Uuid.NIL, entityType = EntityType.CLIENT_FLAG, type = Job.Type.GET)

        scheduler.queue(job = job.copy(conflictPolicy = Job.ConflictPolicy.APPEND))
        scheduler.queue(job = job.copy(conflictPolicy = Job.ConflictPolicy.REPLACE))
        scheduler.queue(job = job.copy(conflictPolicy = Job.ConflictPolicy.IGNORE))
    }

    /** Verifies job store failures during start are swallowed. */
    @Test
    fun startSwallowsJobStoreFailures() = runUnitTest {
        val scheduler = JobScheduler(jobStore = ThrowingJobStore(), jobFactory = FakeJobFactory(), clockService = FakeClockService())

        scheduler.start()
        scheduler.stop()
    }

    /** Verifies that a job left in the running state before startup is recovered by resetZombieJobs and executed to completion. */
    @Test
    fun resetZombieJobsRecoversARunningJobPresentAtStartup() = runUnitTest {
        val jobStore = LocalJobStore()
        val zombieJob = Job(userUuid = Uuid.NIL, entityType = EntityType.CLIENT_FLAG, type = Job.Type.GET, state = Job.State.RUNNING)
        jobStore.upsertJob(job = zombieJob)
        val jobFactory = FakeJobFactory()
        val scheduler = JobScheduler(jobStore = jobStore, jobFactory = jobFactory, clockService = FakeClockService())

        scheduler.start()
        scheduler.stop()

        assertEquals(expected = Job.State.COMPLETE, actual = jobStore.jobs.value.first { it.uuid == zombieJob.uuid }.state)
    }

    /** Verifies that calling start() a second time while already running is a safe no-op. */
    @Test
    fun startTwiceIsSafe() = runUnitTest {
        val jobStore = LocalJobStore()
        val jobFactory = FakeJobFactory()
        val scheduler = JobScheduler(jobStore = jobStore, jobFactory = jobFactory, clockService = FakeClockService())
        val job = Job(userUuid = Uuid.NIL, entityType = EntityType.CLIENT_FLAG, type = Job.Type.GET)

        scheduler.queue(job = job)
        scheduler.start()
        scheduler.start()
        scheduler.stop()

        assertEquals(expected = listOf(job.uuid), actual = jobFactory.resolvedJobs.map { it.uuid })
        assertEquals(expected = Job.State.COMPLETE, actual = jobStore.jobs.value.first { it.uuid == job.uuid }.state)
    }
}
