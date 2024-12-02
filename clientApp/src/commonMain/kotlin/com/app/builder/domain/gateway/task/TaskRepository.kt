package com.app.builder.domain.gateway.task

import kotlin.uuid.Uuid
import com.app.builder.domain.Task
import com.app.builder.domain.scheduler.Job
import com.app.builder.domain.scheduler.JobResult

/** Defines the repository for tasks. */
interface TaskRepository {

    /** Enqueues tasks synchronization. */
    suspend fun syncTasks()

    /**
     * Enqueues a task synchronization.
     *
     * @param uuid The unique identifier of the task to sync.
     */
    suspend fun syncTask(uuid: Uuid)

    /**
     * Attempts to fetch the latest tasks from the remote server.
     *
     * @param job The job configuration blueprint.
     * @return The [JobResult].
     */
    suspend fun executeSyncTasks(job: Job): JobResult

    /**
     * Attempts to fetch a task from the remote server.
     *
     * @param job The job configuration blueprint.
     * @return The [JobResult].
     */
    suspend fun executeSyncTask(job: Job): JobResult

    /**
     * Inserts a new task or updates an existing one.
     *
     * @param task The [Task] to be inserted or updated.
     */
    suspend fun upsertTask(task: Task)

    /**
     * Executes the background job to upsert a task, handling synchronization with the remote server.
     *
     * @param job The persistent background [Job] responsible for the upsert execution.
     * @return The [JobResult].
     */
    suspend fun executeUpsertTask(job: Job): JobResult
}
