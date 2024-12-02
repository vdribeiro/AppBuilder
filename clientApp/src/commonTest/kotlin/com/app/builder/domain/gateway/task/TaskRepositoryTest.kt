package com.app.builder.domain.gateway.task

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.uuid.Uuid
import kotlinx.coroutines.flow.firstOrNull
import com.app.builder.Dependency.getUserDependency
import com.app.builder.core.config.ClientFlags
import com.app.builder.data.serializer.encode
import com.app.builder.data.storage.AppFile
import com.app.builder.domain.EntityType
import com.app.builder.domain.scheduler.Job
import com.app.builder.domain.scheduler.JobResult
import com.app.builder.test.FakeData
import com.app.builder.test.TestCase

class TaskRepositoryTest: TestCase() {

    /** Verifies that executing a tasks sync job succeeds, records the last sync timestamp, and populates the task list. */
    @Test
    fun executeSyncTasks() = runUnitTest {
        val taskUseCases = dependency.get()
            .getUserDependency(user = FakeData.adminUser)
            .useCases
            .taskUseCases
        val taskRepository = dependency.get()
            .getUserDependency(user = FakeData.adminUser)
            .repositories
            .taskRepository
        assertNull(actual = AppFile.Sync.cache().value.orEmpty()[AppFile.Sync.Key.TASKS_LAST_SYNC_UTC])
        assertTrue(actual = taskUseCases.observeTasks().firstOrNull().orEmpty().isEmpty())

        val jobResult = taskRepository.executeSyncTasks(job = Job(userUuid = Uuid.NIL, entityType = EntityType.TASK, type = Job.Type.GET))
        assertTrue(actual = jobResult is JobResult.Success)

        assertNotNull(actual = AppFile.Sync.cache().value.orEmpty()[AppFile.Sync.Key.TASKS_LAST_SYNC_UTC])
        assertEquals(expected = listOf(element = FakeData.task), actual = taskUseCases.observeTasks().firstOrNull().orEmpty())
    }

    /** Verifies that executing a tasks sync job fails when the http client flag is disabled. */
    @Test
    fun executeSyncTasksOffline() = runUnitTest {
        ClientFlags.set { it.copy(http = false) }
        val taskRepository = dependency.get()
            .getUserDependency(user = FakeData.adminUser)
            .repositories
            .taskRepository

        val jobResult = taskRepository.executeSyncTasks(job = Job(userUuid = Uuid.NIL, entityType = EntityType.TASK, type = Job.Type.GET))
        assertTrue(actual = jobResult is JobResult.Error)
    }

    /** Verifies that executing an upsert task job fails without a payload and succeeds once the task is encoded into it. */
    @Test
    fun executeUpsertTask() = runUnitTest {
        val taskUseCases = dependency.get()
            .getUserDependency(user = FakeData.adminUser)
            .useCases
            .taskUseCases
        val taskRepository = dependency.get()
            .getUserDependency(user = FakeData.adminUser)
            .repositories
            .taskRepository

        assertNull(actual = taskUseCases.observeTask(uuid = FakeData.task.uuid).firstOrNull())

        val job = Job(userUuid = Uuid.NIL, entityType = EntityType.TASK, type = Job.Type.POST)
        assertTrue(actual = taskRepository.executeUpsertTask(job = job) is JobResult.Error)

        val jobResult = taskRepository.executeUpsertTask(job = job.copy(payload = encode(value = FakeData.task)))
        assertTrue(actual = jobResult is JobResult.Success)

        assertEquals(expected = FakeData.task, actual = taskUseCases.observeTask(uuid = FakeData.task.uuid).firstOrNull())
    }

    /** Verifies that executing an upsert task job fails when the http client flag is disabled. */
    @Test
    fun executeUpsertTaskOffline() = runUnitTest {
        ClientFlags.set { it.copy(http = false) }
        val taskRepository = dependency.get()
            .getUserDependency(user = FakeData.adminUser)
            .repositories
            .taskRepository

        val job = Job(
            userUuid = Uuid.NIL,
            entityType = EntityType.TASK,
            type = Job.Type.POST,
            payload = encode(value = FakeData.task)
        )
        val jobResult = taskRepository.executeUpsertTask(job = job)
        assertTrue(actual = jobResult is JobResult.Error)
    }
}
