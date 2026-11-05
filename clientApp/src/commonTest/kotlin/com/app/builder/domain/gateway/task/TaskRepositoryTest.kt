package com.app.builder.domain.gateway.task

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.hours
import kotlin.uuid.Uuid
import kotlinx.coroutines.flow.firstOrNull
import app.cash.sqldelight.async.coroutines.awaitAsList
import com.app.builder.Dependency.getUserDependency
import com.app.builder.core.config.ClientFlags
import com.app.builder.data.serializer.encode
import com.app.builder.data.storage.AppFile
import com.app.builder.domain.Account
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
            .getUserDependency(account = FakeData.adminAccount)
            .useCases
            .taskUseCases
        val taskRepository = dependency.get()
            .getUserDependency(account = FakeData.adminAccount)
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
            .getUserDependency(account = FakeData.adminAccount)
            .repositories
            .taskRepository

        val jobResult = taskRepository.executeSyncTasks(job = Job(userUuid = Uuid.NIL, entityType = EntityType.TASK, type = Job.Type.GET))
        assertTrue(actual = jobResult is JobResult.Error)
    }

    /** Verifies that executing an upsert task job fails without a payload and succeeds once the task is encoded into it. */
    @Test
    fun executeUpsertTask() = runUnitTest {
        val taskUseCases = dependency.get()
            .getUserDependency(account = FakeData.adminAccount)
            .useCases
            .taskUseCases
        val taskRepository = dependency.get()
            .getUserDependency(account = FakeData.adminAccount)
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
            .getUserDependency(account = FakeData.adminAccount)
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

    /** An account without any permission, so every task operation is refused. */
    private val unauthorizedAccount: Account = FakeData.account.copy(permissions = emptyMap())

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

    /** Verifies that requesting a task list sync and a single task sync queues a get job for each. */
    @Test
    fun syncTasksQueuesGetJobs() = runUnitTest {
        val taskRepository = dependency.get()
            .getUserDependency(account = FakeData.adminAccount)
            .repositories
            .taskRepository

        taskRepository.syncTasks()
        taskRepository.syncTask(uuid = FakeData.task.uuid)

        val jobs = queuedJobs().filter { it.entityType == EntityType.TASK && it.type == Job.Type.GET }
        assertEquals(expected = setOf(null, FakeData.task.uuid), actual = jobs.map { it.entityUuid }.toSet())
    }

    /** Verifies that a user without read permission observes no tasks, queues no sync and fails every sync job. */
    @Test
    fun withoutReadPermission() = runUnitTest {
        val userDependency = dependency.get().getUserDependency(account = unauthorizedAccount)
        val taskUseCases = userDependency.useCases.taskUseCases
        val taskRepository = userDependency.repositories.taskRepository

        assertNull(actual = taskUseCases.observeTasks().firstOrNull())
        assertNull(actual = taskUseCases.observeTask(uuid = FakeData.task.uuid).firstOrNull())

        taskRepository.syncTasks()
        taskRepository.syncTask(uuid = FakeData.task.uuid)
        assertTrue(actual = queuedJobs().isEmpty())

        val job = Job(userUuid = Uuid.NIL, entityUuid = FakeData.task.uuid, entityType = EntityType.TASK, type = Job.Type.GET)
        assertTrue(actual = taskRepository.executeSyncTasks(job = job) is JobResult.Error)
        assertTrue(actual = taskRepository.executeSyncTask(job = job) is JobResult.Error)
    }

    /** Verifies that a tasks sync keeps a local task that is newer than the remote one, and replaces one that is older. */
    @Test
    fun executeSyncTasksResolvesConflictsByModificationTime() = runUnitTest {
        val userDependency = dependency.get().getUserDependency(account = FakeData.adminAccount)
        val taskUseCases = userDependency.useCases.taskUseCases
        val taskRepository = userDependency.repositories.taskRepository
        val job = Job(userUuid = Uuid.NIL, entityType = EntityType.TASK, type = Job.Type.GET)

        val newer = FakeData.task.copy(modifiedAt = FakeData.task.modifiedAt + 1.hours, title = "Local")
        taskUseCases.upsertTask(task = newer)
        assertTrue(actual = taskRepository.executeSyncTasks(job = job) is JobResult.Success)
        assertEquals(expected = newer, actual = taskUseCases.observeTask(uuid = FakeData.task.uuid).firstOrNull())

        val older = FakeData.task.copy(modifiedAt = FakeData.task.modifiedAt - 1.hours, title = "Local")
        taskUseCases.upsertTask(task = older)
        assertTrue(actual = taskRepository.executeSyncTasks(job = job) is JobResult.Success)
        assertEquals(expected = FakeData.task, actual = taskUseCases.observeTask(uuid = FakeData.task.uuid).firstOrNull())
    }

    /** Verifies that a single task sync keeps a local task that is newer than the remote one, and replaces one that is older. */
    @Test
    fun executeSyncTaskResolvesConflictsByModificationTime() = runUnitTest {
        val userDependency = dependency.get().getUserDependency(account = FakeData.adminAccount)
        val taskUseCases = userDependency.useCases.taskUseCases
        val taskRepository = userDependency.repositories.taskRepository
        val job = Job(userUuid = Uuid.NIL, entityUuid = FakeData.task.uuid, entityType = EntityType.TASK, type = Job.Type.GET)

        val newer = FakeData.task.copy(modifiedAt = FakeData.task.modifiedAt + 1.hours, title = "Local")
        taskUseCases.upsertTask(task = newer)
        assertTrue(actual = taskRepository.executeSyncTask(job = job) is JobResult.Success)
        assertEquals(expected = newer, actual = taskUseCases.observeTask(uuid = FakeData.task.uuid).firstOrNull())

        val older = FakeData.task.copy(modifiedAt = FakeData.task.modifiedAt - 1.hours, title = "Local")
        taskUseCases.upsertTask(task = older)
        assertTrue(actual = taskRepository.executeSyncTask(job = job) is JobResult.Success)
        assertEquals(expected = FakeData.task, actual = taskUseCases.observeTask(uuid = FakeData.task.uuid).firstOrNull())
    }

    /** Verifies that a single task sync job without an entity uuid fails. */
    @Test
    fun executeSyncTaskWithoutEntityUuid() = runUnitTest {
        val taskRepository = dependency.get()
            .getUserDependency(account = FakeData.adminAccount)
            .repositories
            .taskRepository

        val jobResult = taskRepository.executeSyncTask(job = Job(userUuid = Uuid.NIL, entityType = EntityType.TASK, type = Job.Type.GET))
        assertTrue(actual = jobResult is JobResult.Error)
    }

    /** Verifies that a task access sync records the last sync timestamp, stores the accesses, and queues a sync of each task the current user was newly granted access to. */
    @Test
    fun executeSyncTaskAccessesQueuesGrantedTasks() = runUnitTest {
        val userDependency = dependency.get().getUserDependency(account = FakeData.account)
        val taskUseCases = userDependency.useCases.taskUseCases
        val taskRepository = userDependency.repositories.taskRepository

        val jobResult = taskRepository.executeSyncTaskAccesses(job = Job(userUuid = Uuid.NIL, entityType = EntityType.TASK_ACCESS, type = Job.Type.GET))
        assertTrue(actual = jobResult is JobResult.Success)

        assertNotNull(actual = AppFile.Sync.cache().value.orEmpty()[AppFile.Sync.Key.TASK_ACCESSES_LAST_SYNC_UTC])
        assertEquals(expected = listOf(element = FakeData.taskAccess), actual = taskUseCases.observeTaskAccesses(taskUuid = FakeData.task.uuid).firstOrNull())
        val jobs = queuedJobs().filter { it.entityType == EntityType.TASK && it.type == Job.Type.GET }
        assertEquals(expected = listOf(FakeData.task.uuid), actual = jobs.map { it.entityUuid })
    }

    /** Verifies that a synced task is visible while the current user's access to it is granted, and hidden once a newer revocation is synced. */
    @Test
    fun revokedTaskAccessHidesTask() = runUnitTest {
        val userDependency = dependency.get().getUserDependency(account = FakeData.account)
        val taskUseCases = userDependency.useCases.taskUseCases
        val taskRepository = userDependency.repositories.taskRepository
        val getTask = Job(userUuid = Uuid.NIL, entityUuid = FakeData.task.uuid, entityType = EntityType.TASK, type = Job.Type.GET)

        assertTrue(actual = taskRepository.executeSyncTaskAccesses(job = Job(userUuid = Uuid.NIL, entityType = EntityType.TASK_ACCESS, type = Job.Type.GET)) is JobResult.Success)
        assertTrue(actual = taskRepository.executeSyncTask(job = getTask) is JobResult.Success)
        assertEquals(expected = FakeData.task, actual = taskUseCases.observeTask(uuid = FakeData.task.uuid).firstOrNull())

        val revoked = FakeData.taskAccess.copy(modifiedAt = FakeData.taskAccess.modifiedAt + 1.hours, deletedAt = FakeData.taskAccess.modifiedAt + 1.hours)
        dependency.get().database.taskUserAccessQueries.upsertTaskUserAccess(TaskUserAccess = revoked.toTaskUserAccessSchema())
        assertNull(actual = taskUseCases.observeTask(uuid = FakeData.task.uuid).firstOrNull())
        assertTrue(actual = taskUseCases.observeTaskAccesses(taskUuid = FakeData.task.uuid).firstOrNull().orEmpty().isEmpty())

        // An older remote grant doesn't undo the newer local revocation.
        assertTrue(actual = taskRepository.executeSyncTaskAccesses(job = Job(userUuid = Uuid.NIL, entityType = EntityType.TASK_ACCESS, type = Job.Type.GET)) is JobResult.Success)
        assertNull(actual = taskUseCases.observeTask(uuid = FakeData.task.uuid).firstOrNull())
    }

    /** Verifies that executing an upsert task access job fails without a payload and stores the server's access once it is encoded into it. */
    @Test
    fun executeUpsertTaskAccess() = runUnitTest {
        val userDependency = dependency.get().getUserDependency(account = FakeData.adminAccount)
        val taskUseCases = userDependency.useCases.taskUseCases
        val taskRepository = userDependency.repositories.taskRepository

        val job = Job(userUuid = Uuid.NIL, entityType = EntityType.TASK_ACCESS, type = Job.Type.POST)
        assertTrue(actual = taskRepository.executeUpsertTaskAccess(job = job) is JobResult.Error)

        assertTrue(actual = taskRepository.executeUpsertTaskAccess(job = job.copy(payload = encode(value = FakeData.taskAccess))) is JobResult.Success)
        assertEquals(expected = listOf(element = FakeData.taskAccess), actual = taskUseCases.observeTaskAccesses(taskUuid = FakeData.task.uuid).firstOrNull())
    }
}
