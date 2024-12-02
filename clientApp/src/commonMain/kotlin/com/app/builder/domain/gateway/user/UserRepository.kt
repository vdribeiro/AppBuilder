package com.app.builder.domain.gateway.user

import kotlin.uuid.Uuid
import com.app.builder.domain.scheduler.Job
import com.app.builder.domain.scheduler.JobResult

/** Defines the repository for users. */
interface UserRepository {

    /** Enqueues users synchronization. */
    suspend fun syncUsers()

    /**
     * Enqueues a user synchronization.
     *
     * @param uuid The unique identifier of the user to sync.
     */
    suspend fun syncUser(uuid: Uuid)

    /**
     * Attempts to fetch the latest users from the remote server.
     *
     * @param job The job configuration blueprint.
     * @return The [JobResult].
     */
    suspend fun executeSyncUsers(job: Job): JobResult

    /**
     * Attempts to fetch a user from the remote server.
     *
     * @param job The job configuration blueprint.
     * @return The [JobResult].
     */
    suspend fun executeSyncUser(job: Job): JobResult

    /**
     * Executes the background job to upsert a user, handling synchronization with the remote server.
     *
     * @param job The persistent background [Job] responsible for the upsert execution.
     * @return The [JobResult].
     */
    suspend fun executeUpsertUser(job: Job): JobResult
}
