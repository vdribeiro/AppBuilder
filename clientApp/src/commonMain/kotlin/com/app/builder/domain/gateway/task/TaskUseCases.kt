package com.app.builder.domain.gateway.task

import kotlin.uuid.Uuid
import kotlinx.coroutines.flow.Flow
import com.app.builder.domain.Task

/** Defines the business workflows for tasks. */
interface TaskUseCases {

    /**
     * Observes the complete list of tasks.
     *
     * @return [Flow] emitting the current list of [Task] objects.
     */
    fun observeTasks(): Flow<List<Task>>

    /**
     * Observes a specific application task by its unique identifier.
     *
     * @param uuid The unique identifier of the task to observe.
     * @return [Flow] emitting the [Task] if found, or null if it does not exist.
     */
    fun observeTask(uuid: Uuid): Flow<Task?>

    /**
     * Inserts a new task or updates an existing one.
     *
     * @param task The [Task] to be inserted or updated.
     */
    suspend fun upsertTask(task: Task)
}
