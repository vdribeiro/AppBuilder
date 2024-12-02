package com.app.builder.domain.usecase.task

import kotlin.time.Instant
import kotlin.uuid.Uuid
import com.app.builder.core.config.ServerConfigs
import com.app.builder.domain.Task

/** Defines the business workflows for managing tasks. */
interface TaskUseCases {

    /**
     * Gets a page of tasks belonging to a user, updated after the given timestamp, newest first.
     *
     * @param userUuid The unique identifier of the user.
     * @param lastSyncUtc The fixed lower bound below which results are already synced.
     * @param cursorUtc The `modifiedAt` of the last task consumed in the previous page, for descending cursor continuation.
     * @param cursorUuid The UUID of the last task consumed in the previous page, tie-breaking [cursorUtc].
     * @param pageSize The maximum number of entries to return, clamped to [ServerConfigs.maxPageSize].
     * @return The list of [Task] objects.
     */
    suspend fun getTasks(
        userUuid: Uuid,
        lastSyncUtc: Instant,
        cursorUtc: Instant?,
        cursorUuid: Uuid?,
        pageSize: Int
    ): List<Task>

    /**
     * Gets a single task by UUID, scoped to a user.
     *
     * @param userUuid The unique identifier of the user.
     * @param uuid The task's unique identifier.
     * @return The [Task], or `null` if it does not exist or does not belong to the user.
     */
    suspend fun getTask(userUuid: Uuid, uuid: Uuid): Task?

    /**
     * Inserts a new task or updates an existing one, and links it to the given user. Returns false if the task already exists and does not belong to the user.
     *
     * @param userUuid The unique identifier of the user.
     * @param task The [Task] to be inserted or updated.
     * @return true if the operation succeeded, false if the user is not authorized or an error occurred.
     */
    suspend fun upsertTask(userUuid: Uuid, task: Task): Boolean
}
