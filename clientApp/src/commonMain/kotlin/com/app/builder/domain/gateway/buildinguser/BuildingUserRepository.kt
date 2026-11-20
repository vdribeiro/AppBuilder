package com.app.builder.domain.gateway.buildinguser

import kotlin.uuid.Uuid
import com.app.builder.domain.BuildingUser
import com.app.builder.domain.scheduler.Job
import com.app.builder.domain.scheduler.JobResult

/** Defines the repository for building users. */
interface BuildingUserRepository {

    /** Enqueues building users synchronization. */
    suspend fun syncBuildingUsers()

    /**
     * Enqueues a building user synchronization.
     *
     * @param uuid The unique identifier of the building user to sync.
     */
    suspend fun syncBuildingUser(uuid: Uuid)

    /**
     * Attempts to fetch the latest building users from the remote server.
     *
     * @param job The job configuration blueprint.
     * @return The [JobResult].
     */
    suspend fun executeSyncBuildingUsers(job: Job): JobResult

    /**
     * Attempts to fetch a building user from the remote server.
     *
     * @param job The job configuration blueprint.
     * @return The [JobResult].
     */
    suspend fun executeSyncBuildingUser(job: Job): JobResult

    /**
     * Inserts a new building user or updates an existing one.
     *
     * @param buildingUser The [BuildingUser] to be inserted or updated.
     */
    suspend fun upsertBuildingUser(buildingUser: BuildingUser)

    /**
     * Executes the background job to upsert a building user, handling synchronization with the remote server.
     *
     * @param job The persistent background [Job] responsible for the upsert execution.
     * @return The [JobResult].
     */
    suspend fun executeUpsertBuildingUser(job: Job): JobResult
}
