package com.app.builder.domain.gateway.task

import kotlin.uuid.Uuid
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.withContext
import io.ktor.client.HttpClient
import app.cash.sqldelight.async.coroutines.awaitAsList
import app.cash.sqldelight.async.coroutines.awaitAsOneOrNull
import com.app.builder.core.config.ClientConfigs
import com.app.builder.core.config.ServerConfigs
import com.app.builder.core.flow.Dispatcher
import com.app.builder.core.locale.now
import com.app.builder.core.telemetry.Telemetry
import com.app.builder.data.database.TaskSchema
import com.app.builder.data.database.TaskUserJoinSchema
import com.app.builder.data.database.asFlow
import com.app.builder.data.database.safeTransaction
import com.app.builder.data.http.HttpRequest
import com.app.builder.data.http.HttpResult
import com.app.builder.data.http.Query
import com.app.builder.data.http.URL
import com.app.builder.data.http.get
import com.app.builder.data.http.getPaginated
import com.app.builder.data.http.post
import com.app.builder.data.serializer.decode
import com.app.builder.data.serializer.encode
import com.app.builder.data.storage.AppFile
import com.app.builder.domain.EntityType
import com.app.builder.domain.Permission
import com.app.builder.domain.Task
import com.app.builder.domain.User
import com.app.builder.domain.scheduler.Job
import com.app.builder.domain.scheduler.JobResult
import com.app.builder.domain.scheduler.JobResult.NoOp.toJobResult
import com.app.builder.domain.scheduler.Scheduler
import com.app.builder.domain.scheduler.toHeaderMap
import database.AppDatabase

/**
 * Gateway implementation for Task Use Cases and Repository.
 *
 * @property user The current user.
 * @property database The SQLite database instance for task data.
 * @property httpClient The HTTP client used for network operations.
 * @property scheduler The job scheduler for background tasks.
 */
class TaskGateway(
    private val user: User,
    private val database: AppDatabase,
    private val httpClient: HttpClient,
    private val scheduler: Scheduler
): TaskUseCases, TaskRepository {

    /** Query interface for the tasks table. */
    private val taskDao = database.taskQueries
    /** Query interface for the task-user join table. */
    private val taskUserJoinDao = database.taskUserJoinQueries

    override fun observeTasks(): Flow<List<Task>> = if (!user.hasPermission(entityType = EntityType.TASK, permission = Permission.READ)) {
        val throwable = Throwable(message = "User does not have ${Permission.READ} permission for ${EntityType.TASK}")
        Telemetry.error(tag = TAG, message = "Invalid permissions", throwable = throwable)
        return emptyFlow()
    } else taskDao.getTasks(userUuid = user.uuid)
        .asFlow { it.toTask() }
        .onStart {
            val job = Job(
                userUuid = user.uuid,
                entityType = EntityType.TASK,
                type = Job.Type.GET,
            )
            scheduler.queue(job = job)
        }
        .flowOn(context = Dispatcher.IO)
        .catch {
            Telemetry.error(tag = TAG, message = "Unable to observe tasks", throwable = it)
            emit(value = emptyList())
        }

    override fun observeTask(uuid: Uuid): Flow<Task?> = if (!user.hasPermission(entityType = EntityType.TASK, permission = Permission.READ)) {
        val throwable = Throwable(message = "User does not have ${Permission.READ} permission for ${EntityType.TASK}")
        Telemetry.error(tag = TAG, message = "Invalid permissions", throwable = throwable)
        return emptyFlow()
    } else taskDao.getTask(userUuid = user.uuid, taskUuid = uuid)
        .asFlow { it.toTask() }
        .map { it.firstOrNull() }
        .catch {
            Telemetry.error(tag = TAG, message = "Unable to observe task", throwable = it)
            emit(value = null)
        }

    override suspend fun upsertTask(task: Task) = withContext(context = Dispatcher.IO) {
        if (!user.hasPermission(entityType = EntityType.TASK, permission = Permission.WRITE)) {
            val throwable = Throwable(message = "User does not have ${Permission.WRITE} permission for ${EntityType.TASK}")
            Telemetry.error(tag = TAG, message = "Invalid permissions", throwable = throwable)
            return@withContext
        }

        database.safeTransaction {
            upsertTaskInDatabase(task = task)
        }.onFailure {
            Telemetry.error(tag = TAG, message = "Unable to upsert task", throwable = it)
        }
        val job = Job(
            userUuid = user.uuid,
            entityUuid = task.uuid,
            entityType = EntityType.TASK,
            type = Job.Type.POST,
            payload = encode(value = task)
        )
        scheduler.queue(job = job)
    }

    override suspend fun syncTasks() = withContext(context = Dispatcher.IO) {
        if (!user.hasPermission(entityType = EntityType.TASK, permission = Permission.READ)) {
            val throwable = Throwable(message = "User does not have ${Permission.READ} permission for ${EntityType.TASK}")
            Telemetry.error(tag = TAG, message = "Invalid permissions", throwable = throwable)
            return@withContext
        }

        val job = Job(
            userUuid = user.uuid,
            entityType = EntityType.TASK,
            type = Job.Type.GET,
        )
        scheduler.queue(job = job)
    }

    override suspend fun syncTask(uuid: Uuid) = withContext(context = Dispatcher.IO) {
        if (!user.hasPermission(entityType = EntityType.TASK, permission = Permission.READ)) {
            val throwable = Throwable(message = "User does not have ${Permission.READ} permission for ${EntityType.TASK}")
            Telemetry.error(tag = TAG, message = "Invalid permissions", throwable = throwable)
            return@withContext
        }

        val job = Job(
            userUuid = user.uuid,
            entityUuid = uuid,
            entityType = EntityType.TASK,
            type = Job.Type.GET,
        )
        scheduler.queue(job = job)
    }

    override suspend fun executeSyncTasks(job: Job): JobResult = withContext(context = Dispatcher.IO) {
        if (!user.hasPermission(entityType = EntityType.TASK, permission = Permission.READ)) {
            val throwable = Throwable(message = "User does not have ${Permission.READ} permission for ${EntityType.TASK}")
            Telemetry.error(tag = TAG, message = "Invalid permissions", throwable = throwable)
            return@withContext throwable.toJobResult()
        }

        val syncStartUtc = now().toString()
        val sync = AppFile.Sync.cache().value.orEmpty()
        val lastSync = sync[AppFile.Sync.Key.TASKS_LAST_SYNC_UTC]
        val queryMap: Map<Query, String> = buildMap { if (!lastSync.isNullOrBlank()) put(key = Query.LastSyncUtc, value = lastSync) }

        val result = httpClient.getPaginated<Task>(
            request = HttpRequest(
                url = URL.Tasks,
                headerMap = job.toHeaderMap(),
                queryMap = queryMap
            ),
            pageSize = ServerConfigs.configs.pageSize,
            cursorOf = { it.modifiedAt.toString() to it.uuid.toString() }
        ) { tasks ->
            database.safeTransaction {
                syncTasksInDatabase(tasks = tasks)
            }.fold(onSuccess = { HttpResult.Success(data = Unit) }, onFailure = { HttpResult.Error(error = it) })
        }

        when (result) {
            is HttpResult.Error -> {
                Telemetry.error(tag = TAG, message = "Unable to sync tasks", throwable = result.error)
                result.error.toJobResult()
            }

            is HttpResult.Success -> {
                AppFile.Sync.save { it.orEmpty().plus(pair = (AppFile.Sync.Key.TASKS_LAST_SYNC_UTC to syncStartUtc)) }
                Telemetry.info(tag = TAG, message = "Successfully fetched tasks")
                JobResult.Success
            }
        }
    }

    override suspend fun executeSyncTask(job: Job): JobResult = withContext(context = Dispatcher.IO) {
        if (!user.hasPermission(entityType = EntityType.TASK, permission = Permission.READ)) {
            val throwable = Throwable(message = "User does not have ${Permission.READ} permission for ${EntityType.TASK}")
            Telemetry.error(tag = TAG, message = "Invalid permissions", throwable = throwable)
            return@withContext throwable.toJobResult()
        }

        val entityUuid = job.entityUuid ?: return@withContext Throwable(message = "Missing entity uuid").toJobResult()

        when (val result = httpClient.get<Task>(
            request = HttpRequest(
                url = URL.Tasks,
                headerMap = job.toHeaderMap()
            ),
            uuid = entityUuid
        )) {
            is HttpResult.Error -> {
                Telemetry.error(tag = TAG, message = "Unable to get remote task $entityUuid", throwable = result.error)
                result.error.toJobResult()
            }

            is HttpResult.Success -> {
                database.safeTransaction {
                    syncTaskInDatabase(task = result.data)
                }.onFailure {
                    Telemetry.error(tag = TAG, message = "Unable to sync task $entityUuid", throwable = it)
                    return@withContext it.toJobResult()
                }
                Telemetry.info(tag = TAG, message = "Successfully fetched task $entityUuid")
                JobResult.Success
            }
        }
    }

    override suspend fun executeUpsertTask(job: Job): JobResult = withContext(context = Dispatcher.IO) {
        if (!user.hasPermission(entityType = EntityType.TASK, permission = Permission.WRITE)) {
            val throwable = Throwable(message = "User does not have ${Permission.WRITE} permission for ${EntityType.TASK}")
            Telemetry.error(tag = TAG, message = "Invalid permissions", throwable = throwable)
            return@withContext throwable.toJobResult()
        }

        val task = job.payload?.let { decode<Task>(value = it) } ?: run {
            val throwable = Throwable(message = "Unable to decode task")
            Telemetry.error(tag = TAG, message = "Invalid task payload", throwable = throwable)
            return@withContext throwable.toJobResult()
        }

        when (val result = httpClient.post<Task, Task>(
            request = HttpRequest(
                url = URL.Tasks,
                headerMap = job.toHeaderMap(),
            ),
            body = task
        )) {
            is HttpResult.Error -> {
                Telemetry.error(tag = TAG, message = "Unable to post task", throwable = result.error)
                result.error.toJobResult()
            }

            is HttpResult.Success -> {
                database.safeTransaction {
                    syncTaskInDatabase(task = result.data)
                }.onFailure {
                    Telemetry.error(tag = TAG, message = "Unable to sync task", throwable = it)
                    return@withContext it.toJobResult()
                }
                Telemetry.info(tag = TAG, message = "Successfully upserted task")
                JobResult.Success
            }
        }
    }

    /**
     * Synchronizes a single remote task with the local database.
     * This function performs a conflict-aware update by fetching the local equivalent of the task and comparing their `modifiedAt` timestamps.
     * The task is only saved to the database if it does not exist locally or if the remote version is newer.
     *
     * @param task The remote [Task] domain model to evaluate and potentially save.
     */
    private suspend fun syncTaskInDatabase(task: Task) {
        val localTask = taskDao.getTask(userUuid = user.uuid, taskUuid = task.uuid).awaitAsOneOrNull()
        if (localTask == null || task.modifiedAt >= localTask.modifiedAt) upsertTaskInDatabase(task = task)
    }

    /**
     * Synchronizes a batch of remote tasks with the local database.
     * This function performs an optimized, conflict-aware synchronization.
     * It first queries the local database in chunks to find existing tasks.
     * It then compares the `modifiedAt` timestamps in memory to determine which tasks require an update.
     * Finally, it upserts the outdated tasks.
     *
     * @param tasks The list of remote [Task] objects to synchronize locally.
     */
    private suspend fun syncTasksInDatabase(tasks: List<Task>) {
        val localTasksMap = getTasksByUuidsFromDatabase(uuids = tasks.map { it.uuid }).associateBy { it.uuid }
        tasks.forEach { task ->
            val localTask = localTasksMap[task.uuid]
            if (localTask == null || task.modifiedAt >= localTask.modifiedAt) upsertTaskInDatabase(task = task)
        }
    }

    /**
     * Retrieves tasks matching the given UUIDs from the local database, in chunks.
     * SQLite limits the number of bound parameters allowed in a single statement, so large lists are split into batches before querying.
     *
     * @param uuids The task UUIDs to look up, scoped to the currently active user.
     * @return The matching [TaskSchema] entities, merged across all chunks.
     */
    private suspend fun getTasksByUuidsFromDatabase(uuids: List<Uuid>): List<TaskSchema> = buildList {
        uuids.chunked(size = ClientConfigs.configs.batchSize).forEach { uuidChunk ->
            addAll(elements = taskDao.getTasksByUuids(userUuid = user.uuid, taskUuids = uuidChunk).awaitAsList())
        }
    }

    /**
     * Inserts or updates a task and its user association in the local database.
     *
     * @param task The [Task] domain model containing the details to save.
     * @return `true` if the task and its join record were successfully upserted; `false` if no rows were affected.
     */
    private suspend fun upsertTaskInDatabase(task: Task): Boolean =
        (taskDao.upsertTask(Task = task.toTaskSchema())
                + taskUserJoinDao.upsertTaskUserJoin(TaskUserJoin = TaskUserJoinSchema(taskUuid = task.uuid, userUuid = user.uuid))) > 0

    companion object {
        private const val TAG = "TaskGateway"
    }
}
