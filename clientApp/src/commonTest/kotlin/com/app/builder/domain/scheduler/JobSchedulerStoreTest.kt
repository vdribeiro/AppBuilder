package com.app.builder.domain.scheduler

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.flow.firstOrNull
import app.cash.sqldelight.async.coroutines.awaitAsList
import com.app.builder.core.locale.now
import com.app.builder.core.security.uuid
import com.app.builder.domain.EntityType
import com.app.builder.test.FakeData
import com.app.builder.test.TestCase

class JobSchedulerStoreTest: TestCase() {

    /**
     * Builds a store over the test database.
     *
     * @return The [JobSchedulerStore] under test.
     */
    private suspend fun jobStore(): JobSchedulerStore = JobSchedulerStore(database = dependency.get().database)

    /** Verifies that only pending jobs are observed, oldest first. */
    @Test
    fun observePendingJobs() = runUnitTest {
        val jobStore = jobStore()
        val first = Job(userUuid = FakeData.adminUser.uuid, entityType = EntityType.TASK, type = Job.Type.GET)
        val second = Job(userUuid = FakeData.adminUser.uuid, entityType = EntityType.USER, type = Job.Type.GET, utc = first.utc + 1.seconds)
        val running = Job(userUuid = FakeData.adminUser.uuid, entityType = EntityType.REGISTRY, type = Job.Type.GET, state = Job.State.RUNNING)

        assertTrue(actual = jobStore.observePendingJobs().firstOrNull().orEmpty().isEmpty())

        assertTrue(actual = jobStore.upsertJob(job = second))
        assertTrue(actual = jobStore.upsertJob(job = running))
        assertTrue(actual = jobStore.upsertJob(job = first))

        assertEquals(expected = listOf(first, second), actual = jobStore.observePendingJobs().firstOrNull())
    }

    /** Verifies that upserting a job with an existing uuid replaces it. */
    @Test
    fun upsertJob() = runUnitTest {
        val jobStore = jobStore()
        val job = Job(userUuid = FakeData.adminUser.uuid, entityType = EntityType.TASK, type = Job.Type.POST, payload = "first")

        assertTrue(actual = jobStore.upsertJob(job = job))
        assertTrue(actual = jobStore.upsertJob(job = job.copy(payload = "second")))

        assertEquals(expected = listOf(job.copy(payload = "second")), actual = jobStore.observePendingJobs().firstOrNull())
    }

    /** Verifies that duplicates are counted among pending jobs sharing the user, entity, entity type and type, with and without an entity uuid. */
    @Test
    fun getDuplicateJobsCount() = runUnitTest {
        val jobStore = jobStore()
        val sync = Job(userUuid = FakeData.adminUser.uuid, entityType = EntityType.TASK, type = Job.Type.GET)
        val get = Job(userUuid = FakeData.adminUser.uuid, entityUuid = FakeData.task.uuid, entityType = EntityType.TASK, type = Job.Type.GET)

        assertEquals(expected = 0L, actual = jobStore.getDuplicateJobsCount(job = sync))

        jobStore.upsertJob(job = sync)
        jobStore.upsertJob(job = sync.copy(uuid = uuid()))
        jobStore.upsertJob(job = sync.copy(uuid = uuid(), state = Job.State.COMPLETE))
        jobStore.upsertJob(job = sync.copy(uuid = uuid(), userUuid = FakeData.user.uuid))
        jobStore.upsertJob(job = sync.copy(uuid = uuid(), type = Job.Type.POST))
        jobStore.upsertJob(job = get)

        assertEquals(expected = 2L, actual = jobStore.getDuplicateJobsCount(job = sync))
        assertEquals(expected = 1L, actual = jobStore.getDuplicateJobsCount(job = get))
    }

    /** Verifies that canceling a job's pending duplicates marks only those as canceled, and reports whether any was found. */
    @Test
    fun cancelPendingJobs() = runUnitTest {
        val jobStore = jobStore()
        val jobDao = dependency.get().database.jobQueries
        val job = Job(userUuid = FakeData.adminUser.uuid, entityUuid = FakeData.task.uuid, entityType = EntityType.TASK, type = Job.Type.POST)
        val other = Job(userUuid = FakeData.adminUser.uuid, entityUuid = FakeData.task.uuid, entityType = EntityType.TASK, type = Job.Type.DELETE)

        assertFalse(actual = jobStore.cancelPendingJobs(job = job))

        jobStore.upsertJob(job = job)
        jobStore.upsertJob(job = other)

        assertTrue(actual = jobStore.cancelPendingJobs(job = job))
        assertEquals(expected = listOf(other), actual = jobStore.observePendingJobs().firstOrNull())
        assertEquals(expected = listOf(job.copy(state = Job.State.CANCELED)), actual = jobDao.getJobsByState(states = listOf(Job.State.CANCELED)).awaitAsList().map { it.toJob() })
    }

    /** Verifies that jobs left running are put back to pending, and that the count of reset jobs is returned. */
    @Test
    fun resetZombieJobs() = runUnitTest {
        val jobStore = jobStore()
        val running = Job(userUuid = FakeData.adminUser.uuid, entityType = EntityType.TASK, type = Job.Type.GET, state = Job.State.RUNNING)
        val failed = Job(userUuid = FakeData.adminUser.uuid, entityType = EntityType.USER, type = Job.Type.GET, state = Job.State.FAILED)

        jobStore.upsertJob(job = running)
        jobStore.upsertJob(job = failed)

        assertEquals(expected = 1L, actual = jobStore.resetZombieJobs())
        assertEquals(expected = listOf(running.copy(state = Job.State.PENDING)), actual = jobStore.observePendingJobs().firstOrNull())
        assertEquals(expected = 0L, actual = jobStore.resetZombieJobs())
    }

    /** Verifies that a job's state, attempt and next attempt are updated by uuid, and that an unknown job reports no update. */
    @Test
    fun updateJobStateAndAttempt() = runUnitTest {
        val jobStore = jobStore()
        val jobDao = dependency.get().database.jobQueries
        val job = Job(userUuid = FakeData.adminUser.uuid, entityType = EntityType.TASK, type = Job.Type.GET)

        assertFalse(actual = jobStore.updateJobStateAndAttempt(job = job))

        jobStore.upsertJob(job = job)
        val retried = job.copy(state = Job.State.FAILED, attempt = 2, nextAttemptAt = now())
        assertTrue(actual = jobStore.updateJobStateAndAttempt(job = retried))

        assertTrue(actual = jobStore.observePendingJobs().firstOrNull().orEmpty().isEmpty())
        assertEquals(expected = listOf(retried), actual = jobDao.getJobsByState(states = listOf(Job.State.FAILED)).awaitAsList().map { it.toJob() })
    }
}
