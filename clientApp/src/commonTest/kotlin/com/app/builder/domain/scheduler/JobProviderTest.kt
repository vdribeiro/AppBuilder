package com.app.builder.domain.scheduler

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.uuid.Uuid
import com.app.builder.domain.EntityType
import com.app.builder.test.TestCase

class JobProviderTest: TestCase() {

    /**
     * Resolves [job] through the app's job factory and runs it.
     *
     * @param job The job to resolve.
     * @return The [JobResult] of running the resolved job.
     */
    private suspend fun run(job: Job): JobResult = dependency.get().jobFactory.resolve(job = job).invoke()

    /**
     * Builds a guest job, which resolves to the guest user's repositories.
     *
     * @param type The job type.
     * @param entityType The entity type the job targets.
     * @param entityUuid The entity the job targets, or null for every entity of [entityType].
     * @return The [Job] instance.
     */
    private fun guestJob(type: Job.Type, entityType: EntityType, entityUuid: Uuid? = null): Job =
        Job(userUuid = Uuid.NIL, entityUuid = entityUuid, entityType = entityType, type = type)

    /** Verifies that every handled job is dispatched to a repository rather than resolving to a no-op. */
    @Test
    fun handledJobsAreDispatched() = runUnitTest {
        val jobs = listOf(
            guestJob(type = Job.Type.GET, entityType = EntityType.CLIENT_FLAG),
            guestJob(type = Job.Type.GET, entityType = EntityType.CLIENT_CONFIG),
            guestJob(type = Job.Type.GET, entityType = EntityType.REGISTRY),
            guestJob(type = Job.Type.GET, entityType = EntityType.TRANSLATION),
            guestJob(type = Job.Type.GET, entityType = EntityType.USER),
            guestJob(type = Job.Type.GET, entityType = EntityType.TASK),
            guestJob(type = Job.Type.GET, entityType = EntityType.USER, entityUuid = Uuid.random()),
            guestJob(type = Job.Type.GET, entityType = EntityType.TASK, entityUuid = Uuid.random()),
            guestJob(type = Job.Type.POST, entityType = EntityType.SESSION),
            guestJob(type = Job.Type.POST, entityType = EntityType.USER),
            guestJob(type = Job.Type.POST, entityType = EntityType.TASK),
            guestJob(type = Job.Type.DELETE, entityType = EntityType.SESSION),
        )

        jobs.forEach { job ->
            assertNotEquals(illegal = JobResult.NoOp, actual = run(job = job), message = "$job was not dispatched")
        }
    }

    /** Verifies that jobs for entity types without a handler resolve to a no-op. */
    @Test
    fun unhandledJobsAreNoOps() = runUnitTest {
        val jobs = listOf(
            guestJob(type = Job.Type.GET, entityType = EntityType.NOTIFICATION),
            guestJob(type = Job.Type.GET, entityType = EntityType.REGISTRY, entityUuid = Uuid.random()),
            guestJob(type = Job.Type.POST, entityType = EntityType.REGISTRY),
            guestJob(type = Job.Type.DELETE, entityType = EntityType.TASK),
        )

        jobs.forEach { job ->
            assertEquals(expected = JobResult.NoOp, actual = run(job = job), message = "$job was dispatched")
        }
    }

    /** Verifies that jobs of a user that is not stored resolve to a no-op, whatever their type. */
    @Test
    fun jobsOfAnUnknownUserAreNoOps() = runUnitTest {
        val userUuid = Uuid.random()
        val jobs = listOf(
            Job(userUuid = userUuid, entityType = EntityType.TASK, type = Job.Type.GET),
            Job(userUuid = userUuid, entityUuid = Uuid.random(), entityType = EntityType.TASK, type = Job.Type.GET),
            Job(userUuid = userUuid, entityType = EntityType.TASK, type = Job.Type.POST),
            Job(userUuid = userUuid, entityType = EntityType.SESSION, type = Job.Type.DELETE),
        )

        jobs.forEach { job ->
            assertEquals(expected = JobResult.NoOp, actual = run(job = job), message = "$job was dispatched")
        }
    }
}
