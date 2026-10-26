package com.app.builder.domain.scheduler

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.seconds
import kotlin.uuid.Uuid
import kotlinx.coroutines.flow.first
import com.app.builder.core.locale.now
import com.app.builder.core.security.uuid
import com.app.builder.domain.EntityType
import com.app.builder.test.TestCase

class LocalJobStoreTest: TestCase() {

    /** Verifies that upserting a job with an existing uuid replaces it instead of adding a second entry. */
    @Test
    fun upsertReplacesJobWithTheSameUuid() = runUnitTest {
        val jobStore = LocalJobStore()
        val job = Job(userUuid = Uuid.NIL, entityType = EntityType.TASK, type = Job.Type.POST, payload = "first")

        assertTrue(actual = jobStore.upsertJob(job = job))
        assertTrue(actual = jobStore.upsertJob(job = job.copy(payload = "second")))

        assertEquals(expected = listOf(job.copy(payload = "second")), actual = jobStore.jobs.value)
    }

    /** Verifies that [LocalJobStore.observePendingJobs] only emits jobs in the pending state. */
    @Test
    fun observePendingJobsOnlyEmitsPendingJobs() = runUnitTest {
        val jobStore = LocalJobStore()
        val pending = Job(userUuid = Uuid.NIL, entityType = EntityType.TASK, type = Job.Type.POST)
        Job.State.entries.forEach { state -> jobStore.upsertJob(job = pending.copy(uuid = uuid(), state = state)) }
        jobStore.upsertJob(job = pending)

        val observed = jobStore.observePendingJobs().first()

        assertEquals(expected = 2, actual = observed.size)
        assertTrue(actual = observed.all { it.state == Job.State.PENDING })
    }

    /** Verifies that cancelling pending jobs only cancels pending jobs matching the user, entity type, entity uuid and job type of the blueprint. */
    @Test
    fun cancelPendingJobsOnlyCancelsMatchingPendingJobs() = runUnitTest {
        val jobStore = LocalJobStore()
        val blueprint = Job(userUuid = uuid(), entityUuid = uuid(), entityType = EntityType.TASK, type = Job.Type.POST)
        val matching = blueprint.copy(uuid = uuid())
        val otherUser = blueprint.copy(uuid = uuid(), userUuid = uuid())
        val otherEntity = blueprint.copy(uuid = uuid(), entityUuid = uuid())
        val otherEntityType = blueprint.copy(uuid = uuid(), entityType = EntityType.USER)
        val otherType = blueprint.copy(uuid = uuid(), type = Job.Type.DELETE)
        val running = blueprint.copy(uuid = uuid(), state = Job.State.RUNNING)
        listOf(matching, otherUser, otherEntity, otherEntityType, otherType, running).forEach { jobStore.upsertJob(job = it) }

        assertTrue(actual = jobStore.cancelPendingJobs(job = blueprint))

        val states = jobStore.jobs.value.associate { it.uuid to it.state }
        assertEquals(expected = Job.State.CANCELED, actual = states[matching.uuid])
        assertEquals(expected = Job.State.PENDING, actual = states[otherUser.uuid])
        assertEquals(expected = Job.State.PENDING, actual = states[otherEntity.uuid])
        assertEquals(expected = Job.State.PENDING, actual = states[otherEntityType.uuid])
        assertEquals(expected = Job.State.PENDING, actual = states[otherType.uuid])
        assertEquals(expected = Job.State.RUNNING, actual = states[running.uuid])
    }

    /** Verifies that the duplicate count only includes pending jobs sharing the blueprint's user, entity type, entity uuid and job type. */
    @Test
    fun getDuplicateJobsCountOnlyCountsMatchingPendingJobs() = runUnitTest {
        val jobStore = LocalJobStore()
        val blueprint = Job(userUuid = uuid(), entityType = EntityType.TASK, type = Job.Type.GET)
        jobStore.upsertJob(job = blueprint.copy(uuid = uuid()))
        jobStore.upsertJob(job = blueprint.copy(uuid = uuid()))
        jobStore.upsertJob(job = blueprint.copy(uuid = uuid(), state = Job.State.COMPLETE))
        jobStore.upsertJob(job = blueprint.copy(uuid = uuid(), entityUuid = uuid()))
        jobStore.upsertJob(job = blueprint.copy(uuid = uuid(), type = Job.Type.POST))

        assertEquals(expected = 2L, actual = jobStore.getDuplicateJobsCount(job = blueprint))
    }

    /** Verifies that resetting zombie jobs moves every running job back to pending and reports how many were reset. */
    @Test
    fun resetZombieJobsRequeuesRunningJobs() = runUnitTest {
        val jobStore = LocalJobStore()
        val job = Job(userUuid = Uuid.NIL, entityType = EntityType.TASK, type = Job.Type.POST)
        val running = listOf(job.copy(uuid = uuid(), state = Job.State.RUNNING), job.copy(uuid = uuid(), state = Job.State.RUNNING))
        val complete = job.copy(uuid = uuid(), state = Job.State.COMPLETE)
        (running + complete).forEach { jobStore.upsertJob(job = it) }

        assertEquals(expected = 2L, actual = jobStore.resetZombieJobs())

        val states = jobStore.jobs.value.associate { it.uuid to it.state }
        running.forEach { assertEquals(expected = Job.State.PENDING, actual = states[it.uuid]) }
        assertEquals(expected = Job.State.COMPLETE, actual = states[complete.uuid])
    }

    /** Verifies that updating a job's state and attempt only copies the state, attempt and retry deadline, leaving the rest of the stored job untouched. */
    @Test
    fun updateJobStateAndAttemptOnlyUpdatesExecutionFields() = runUnitTest {
        val jobStore = LocalJobStore()
        val stored = Job(userUuid = Uuid.NIL, entityType = EntityType.TASK, type = Job.Type.POST, payload = "stored")
        val other = stored.copy(uuid = uuid())
        jobStore.upsertJob(job = stored)
        jobStore.upsertJob(job = other)
        val deadline = now() + 5.seconds

        assertTrue(actual = jobStore.updateJobStateAndAttempt(job = stored.copy(state = Job.State.FAILED, attempt = 3, nextAttemptAt = deadline, payload = "ignored")))

        val updated = jobStore.jobs.value.first { it.uuid == stored.uuid }
        assertEquals(expected = stored.copy(state = Job.State.FAILED, attempt = 3, nextAttemptAt = deadline), actual = updated)
        assertEquals(expected = other, actual = jobStore.jobs.value.first { it.uuid == other.uuid })
    }
}
