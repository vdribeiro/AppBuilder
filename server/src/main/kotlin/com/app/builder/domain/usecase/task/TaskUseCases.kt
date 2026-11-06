package com.app.builder.domain.usecase.task

import kotlin.time.Instant
import kotlin.uuid.Uuid
import com.app.builder.core.config.ServerConfigs
import com.app.builder.domain.Task

/** Defines the business workflows for managing tasks. */
interface TaskUseCases {

    /**
     * Gets a page of tasks updated after the given timestamp, newest first.
     *
     * @param lastSyncUtc The fixed lower bound below which results are already synced.
     * @param cursorUtc The `modifiedAt` of the last task consumed in the previous page, for descending cursor continuation.
     * @param cursorUuid The UUID of the last task consumed in the previous page, tie-breaking [cursorUtc].
     * @param pageSize The maximum number of entries to return, clamped to [ServerConfigs.maxPageSize].
     * @return The list of [Task] objects.
     */
    suspend fun getTasks(
        lastSyncUtc: Instant,
        cursorUtc: Instant?,
        cursorUuid: Uuid?,
        pageSize: Int
    ): List<Task>

    /**
     * Gets a single task by UUID.
     *
     * @param uuid The task's unique identifier.
     * @return The [Task], or `null` if it does not exist.
     */
    suspend fun getTask(uuid: Uuid): Task?

    /**
     * Inserts a new task or updates an existing one.
     *
     * @param task The [Task] to be inserted or updated.
     * @return true if the operation succeeded, false if an error occurred.
     */
    suspend fun upsertTask(task: Task): Boolean
}
