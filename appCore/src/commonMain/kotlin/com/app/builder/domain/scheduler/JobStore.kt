package com.app.builder.domain.scheduler

import kotlinx.coroutines.flow.Flow

/** Persistence contract for the background job queue. */
interface JobStore {

    /**
     * Observes jobs currently awaiting execution.
     *
     * @return A flow of pending jobs.
     */
    fun observePendingJobs(): Flow<List<Job>>

    /**
     * Inserts or updates a job in storage.
     *
     * @param job The [Job] to save.
     * @return `true` if the job was successfully upserted; `false` if an error occurred or no rows were affected.
     */
    suspend fun upsertJob(job: Job): Boolean

    /**
     * Cancels pending jobs sharing the same blueprint as [job].
     *
     * @param job The job configuration blueprint.
     * @return `true` if successful; `false` if an error occurred or no rows were affected.
     */
    suspend fun cancelPendingJobs(job: Job): Boolean

    /**
     * Counts pending jobs that duplicate the given [job] blueprint.
     *
     * @param job The job configuration blueprint.
     * @return The count of duplicate jobs.
     */
    suspend fun getDuplicateJobsCount(job: Job): Long

    /**
     * Resets any [Job.State.RUNNING] jobs back to [Job.State.PENDING].
     *
     * @return The number of rows updated.
     */
    suspend fun resetZombieJobs(): Long

    /**
     * Updates a job's state and attempt count in storage.
     *
     * @param job The job to update.
     * @return `true` if successfully updated; `false` if an error occurred or no rows were affected.
     */
    suspend fun updateJobStateAndAttempt(job: Job): Boolean
}
