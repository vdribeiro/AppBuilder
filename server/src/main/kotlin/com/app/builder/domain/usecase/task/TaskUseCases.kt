package com.app.builder.domain.usecase.task

import kotlin.time.Instant
import kotlin.uuid.Uuid
import com.app.builder.core.config.ServerConfigs
import com.app.builder.domain.Task
import com.app.builder.domain.usecase.Outcome

/** Defines the business workflows for managing tasks. */
interface TaskUseCases {

    /**
     * Gets a page of tasks updated after the given timestamp, newest first.
     *
     * @param lastSyncUtc The fixed lower bound below which results are already synced.
     * @param cursorUtc The `modifiedAt` of the last task consumed in the previous page, for descending cursor continuation.
     * @param cursorUuid The UUID of the last task consumed in the previous page, tie-breaking [cursorUtc].
     * @param pageSize The maximum number of entries to return, clamped to [ServerConfigs.maxPageSize].
     * @return [Outcome.Success] with the [Task] objects, or [Outcome.Error] when the operation failed.
     */
    suspend fun getTasks(
        lastSyncUtc: Instant,
        cursorUtc: Instant?,
        cursorUuid: Uuid?,
        pageSize: Int
    ): Outcome<List<Task>>

    /**
     * Gets a single task by UUID.
     *
     * @param uuid The task's unique identifier.
     * @return [Outcome.Success] with the [Task], [Outcome.NotFound] when it does not exist, or [Outcome.Error] when the operation failed.
     */
    suspend fun getTask(uuid: Uuid): Outcome<Task>

    /**
     * Inserts a new task or updates an existing one.
     *
     * @param task The [Task] to be inserted or updated.
     * @return [Outcome.Success] when persisted, or [Outcome.Error] when the operation failed.
     */
    suspend fun upsertTask(task: Task): Outcome<Unit>
}
