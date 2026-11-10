package com.app.builder.domain.usecase.task

import kotlin.time.Instant
import kotlin.uuid.Uuid
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.withContext
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.greaterEq
import org.jetbrains.exposed.v1.core.isNull
import org.jetbrains.exposed.v1.core.less
import org.jetbrains.exposed.v1.core.or
import org.jetbrains.exposed.v1.r2dbc.R2dbcDatabase
import org.jetbrains.exposed.v1.r2dbc.select
import org.jetbrains.exposed.v1.r2dbc.selectAll
import org.jetbrains.exposed.v1.r2dbc.upsert
import com.app.builder.core.config.ServerConfigs
import com.app.builder.core.flow.Dispatcher
import com.app.builder.core.security.uuid
import com.app.builder.core.telemetry.Telemetry
import com.app.builder.data.database.safeTransaction
import com.app.builder.data.database.table.TaskTable
import com.app.builder.data.database.table.UserAccountTable
import com.app.builder.data.serializer.decode
import com.app.builder.domain.EntityType
import com.app.builder.domain.Permission
import com.app.builder.domain.PushPayload
import com.app.builder.domain.Task
import com.app.builder.domain.usecase.Outcome
import com.app.builder.domain.usecase.notification.NotificationsUseCases
import com.app.builder.domain.usecase.toWriteOutcome
import com.app.builder.toEnumOrNull

/**
 * Gateway implementation for tasks use cases.
 *
 * @property database The database instance used to execute transactions.
 * @property notificationsUseCases Used to save a notification for the users that can read tasks whenever a task is upserted.
 */
class TaskGateway(
    private val database: R2dbcDatabase,
    private val notificationsUseCases: NotificationsUseCases
): TaskUseCases {

    override suspend fun getTasks(
        lastSyncUtc: Instant,
        cursorUtc: Instant?,
        cursorUuid: Uuid?,
        pageSize: Int
    ): Outcome<List<Task>> = withContext(context = Dispatcher.IO) {
        val effectivePageSize = pageSize.coerceIn(minimumValue = 1, maximumValue = ServerConfigs.value.maxPageSize)
        database.safeTransaction {
            TaskTable
                .selectAll()
                .where {
                    val sinceLastSync = TaskTable.modifiedAt greaterEq lastSyncUtc
                    when {
                        cursorUtc == null || cursorUuid == null -> sinceLastSync
                        else -> sinceLastSync and ((TaskTable.modifiedAt less cursorUtc) or ((TaskTable.modifiedAt eq cursorUtc) and (TaskTable.uuid less cursorUuid)))
                    }
                }
                .orderBy(TaskTable.modifiedAt to SortOrder.DESC, TaskTable.uuid to SortOrder.DESC)
                .limit(count = effectivePageSize)
                .toList()
                .mapNotNull { it.toTask() }
                .let { Outcome.Success(data = it) }
        }.getOrElse {
            Telemetry.error(tag = TAG, message = "Unable to get tasks since $lastSyncUtc", throwable = it)
            Outcome.Error
        }
    }

    override suspend fun getTask(uuid: Uuid): Outcome<Task> = withContext(context = Dispatcher.IO) {
        database.safeTransaction {
            TaskTable
                .selectAll()
                .where { TaskTable.uuid eq uuid }
                .firstOrNull()
                ?.toTask()
                ?.let { Outcome.Success(data = it) }
                ?: Outcome.NotFound
        }.getOrElse {
            Telemetry.error(tag = TAG, message = "Unable to get task $uuid", throwable = it)
            Outcome.Error
        }
    }

    override suspend fun upsertTask(task: Task): Outcome<Unit> = withContext(context = Dispatcher.IO) {
        val result = database.safeTransaction {
            TaskTable.upsert {
                it[TaskTable.uuid] = task.uuid
                it[TaskTable.modifiedAt] = task.modifiedAt
                it[TaskTable.deletedAt] = task.deletedAt
                it[TaskTable.title] = task.title
                it[TaskTable.description] = task.description
                it[TaskTable.state] = task.state.name
            }.insertedCount > 0
        }.onFailure {
            Telemetry.error(tag = TAG, message = "Unable to upsert task ${task.uuid}", throwable = it)
        }.getOrDefault(defaultValue = false).toWriteOutcome()

        saveNotification(task = task)

        return@withContext result
    }

    /**
     * Persists a notification to the users with an active account that can read tasks.
     * If the notification service is active, it will trigger a push notification to the users.
     *
     * @param task The [task] to use for the notification.
     */
    private suspend fun saveNotification(task: Task) {
        val users = database.safeTransaction {
            UserAccountTable
                .select(UserAccountTable.userUuid, UserAccountTable.permissions)
                .where { UserAccountTable.deletedAt.isNull() }
                .toList()
                .filter { decode<Map<EntityType, Permission>>(value = it[UserAccountTable.permissions])?.get(EntityType.TASK) != null }
                .map { it[UserAccountTable.userUuid] }
        }.onFailure {
            Telemetry.error(tag = TAG, message = "Unable to get users of this task", throwable = it)
        }.getOrDefault(defaultValue = emptyList())

        users.forEach { userUuid ->
            notificationsUseCases.saveNotification(
                PushPayload.EntityNotification(
                    uuid = uuid(),
                    userUuid = userUuid,
                    title = "task",
                    description = "task_updated",
                    entityType = EntityType.TASK,
                    entityUuid = task.uuid,
                ),
            )
        }
    }

    /**
     * Maps a database row to a [Task] domain model.
     *
     * @receiver A row from the [TaskTable].
     * @return The mapped [Task], or `null` if the stored state is unrecognized.
     */
    private fun ResultRow.toTask(): Task? = Task(
        uuid = this[TaskTable.uuid],
        modifiedAt = this[TaskTable.modifiedAt],
        deletedAt = this[TaskTable.deletedAt],
        title = this[TaskTable.title],
        description = this[TaskTable.description],
        state = this[TaskTable.state].toEnumOrNull() ?: run {
            Telemetry.error(tag = TAG, message = "Invalid task state: ${this[TaskTable.state]}")
            return null
        }
    )

    companion object {
        private const val TAG = "TaskGateway"
    }
}
