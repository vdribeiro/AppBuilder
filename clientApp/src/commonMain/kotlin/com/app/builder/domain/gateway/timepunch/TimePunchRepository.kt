package com.app.builder.domain.gateway.timepunch

import kotlin.uuid.Uuid
import com.app.builder.domain.TimePunch
import com.app.builder.domain.scheduler.Job
import com.app.builder.domain.scheduler.JobResult

/** Defines the repository for timePunches. */
interface TimePunchRepository {

    /** Enqueues time punches synchronization. */
    suspend fun syncTimePunches()

    /**
     * Enqueues a time punch synchronization.
     *
     * @param uuid The unique identifier of the time punch to sync.
     */
    suspend fun syncTimePunch(uuid: Uuid)

    /**
     * Attempts to fetch the latest time punches from the remote server.
     *
     * @param job The job configuration blueprint.
     * @return The [JobResult].
     */
    suspend fun executeSyncTimePunches(job: Job): JobResult

    /**
     * Attempts to fetch a time punch from the remote server.
     *
     * @param job The job configuration blueprint.
     * @return The [JobResult].
     */
    suspend fun executeSyncTimePunch(job: Job): JobResult

    /**
     * Inserts a new time punch or updates an existing one.
     *
     * @param timePunch The [TimePunch] to be inserted or updated.
     */
    suspend fun upsertTimePunch(timePunch: TimePunch)

    /**
     * Executes the background job to upsert a time punch, handling synchronization with the remote server.
     *
     * @param job The persistent background [Job] responsible for the upsert execution.
     * @return The [JobResult].
     */
    suspend fun executeUpsertTimePunch(job: Job): JobResult
}
