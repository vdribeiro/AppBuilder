package com.app.builder.domain.gateway.task

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.flow.firstOrNull
import app.cash.sqldelight.async.coroutines.awaitAsList
import com.app.builder.Dependency.getUserDependency
import com.app.builder.core.locale.now
import com.app.builder.core.security.uuid
import com.app.builder.data.serializer.decode
import com.app.builder.domain.EntityType
import com.app.builder.domain.TaskUserAccess
import com.app.builder.domain.scheduler.Job
import com.app.builder.test.FakeData
import com.app.builder.test.TestCase

class TaskUseCasesTest: TestCase() {

    /** Verifies that upserting a task makes it observable both individually and in the task list. */
    @Test
    fun crudTask() = runUnitTest {
        val taskUseCases = dependency.get()
            .getUserDependency(account = FakeData.adminAccount)
            .useCases
            .taskUseCases

        assertTrue(actual = taskUseCases.observeTasks().firstOrNull().orEmpty().isEmpty())
        taskUseCases.upsertTask(task = FakeData.task)
        assertEquals(expected = listOf(element = FakeData.task), actual = taskUseCases.observeTasks().firstOrNull().orEmpty())
        assertEquals(expected = FakeData.task, actual = taskUseCases.observeTask(uuid = FakeData.task.uuid).firstOrNull())
    }

    /** Verifies that the task list includes the deleted tasks. */
    @Test
    fun filterTasks() = runUnitTest {
        val taskUseCases = dependency.get()
            .getUserDependency(account = FakeData.adminAccount)
            .useCases
            .taskUseCases
        val deletedTask = FakeData.task.copy(uuid = uuid(), deletedAt = now())

        taskUseCases.upsertTask(task = FakeData.task)
        taskUseCases.upsertTask(task = deletedTask)

        assertEquals(expected = setOf(FakeData.task, deletedTask), actual = taskUseCases.observeTasks().firstOrNull().orEmpty().toSet())
    }

    /**
     * Lists the jobs queued in the scheduler, in every state.
     *
     * @return The queued jobs, oldest first.
     */
    private suspend fun queuedJobs(): List<database.Job> = dependency.get()
        .database
        .jobQueries
        .getJobsByState(states = Job.State.entries)
        .awaitAsList()

    /** Verifies that the creator of a task has access to it, and that granting and revoking another user's access updates the accesses and queues a post job for each change. */
    @Test
    fun grantAndRevokeTaskAccess() = runUnitTest {
        val taskUseCases = dependency.get()
            .getUserDependency(account = FakeData.adminAccount)
            .useCases
            .taskUseCases

        taskUseCases.upsertTask(task = FakeData.task)
        assertEquals(expected = listOf(FakeData.adminUser.uuid), actual = taskUseCases.observeTaskAccesses(taskUuid = FakeData.task.uuid).firstOrNull()?.map { it.userUuid })

        taskUseCases.grantTaskAccess(taskUuid = FakeData.task.uuid, userUuid = FakeData.user.uuid)
        assertEquals(expected = setOf(FakeData.adminUser.uuid, FakeData.user.uuid), actual = taskUseCases.observeTaskAccesses(taskUuid = FakeData.task.uuid).firstOrNull()?.map { it.userUuid }?.toSet())

        // Granting an access that is already granted changes nothing.
        taskUseCases.grantTaskAccess(taskUuid = FakeData.task.uuid, userUuid = FakeData.user.uuid)
        taskUseCases.revokeTaskAccess(taskUuid = FakeData.task.uuid, userUuid = FakeData.user.uuid)
        assertEquals(expected = listOf(FakeData.adminUser.uuid), actual = taskUseCases.observeTaskAccesses(taskUuid = FakeData.task.uuid).firstOrNull()?.map { it.userUuid })

        val payloads = queuedJobs().filter { it.entityType == EntityType.TASK_ACCESS && it.type == Job.Type.POST }.map { decode<TaskUserAccess>(value = it.payload!!)!! }
        assertEquals(expected = 2, actual = payloads.size)
        assertEquals(expected = 1, actual = payloads.map { it.uuid }.toSet().size)
        assertNull(actual = payloads.first().deletedAt)
        assertNotNull(actual = payloads.last().deletedAt)
    }

    /** Verifies that revoking the current user's own access hides the task, and that access can't be changed on a task the current user has no access to. */
    @Test
    fun revokeOwnTaskAccess() = runUnitTest {
        val taskUseCases = dependency.get()
            .getUserDependency(account = FakeData.adminAccount)
            .useCases
            .taskUseCases

        taskUseCases.grantTaskAccess(taskUuid = FakeData.task.uuid, userUuid = FakeData.user.uuid)
        assertTrue(actual = taskUseCases.observeTaskAccesses(taskUuid = FakeData.task.uuid).firstOrNull().orEmpty().isEmpty())

        taskUseCases.upsertTask(task = FakeData.task)
        taskUseCases.revokeTaskAccess(taskUuid = FakeData.task.uuid, userUuid = FakeData.adminUser.uuid)
        assertNull(actual = taskUseCases.observeTask(uuid = FakeData.task.uuid).firstOrNull())
        assertTrue(actual = taskUseCases.observeTasks().firstOrNull().orEmpty().isEmpty())
    }
}
