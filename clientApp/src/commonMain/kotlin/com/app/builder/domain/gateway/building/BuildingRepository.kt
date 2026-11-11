package com.app.builder.domain.gateway.building

import kotlin.uuid.Uuid
import com.app.builder.domain.Building
import com.app.builder.domain.scheduler.Job
import com.app.builder.domain.scheduler.JobResult

/** Defines the repository for buildings. */
interface BuildingRepository {

    /** Enqueues buildings synchronization. */
    suspend fun syncBuildings()

    /**
     * Enqueues a building synchronization.
     *
     * @param uuid The unique identifier of the building to sync.
     */
    suspend fun syncBuilding(uuid: Uuid)

    /**
     * Attempts to fetch the latest buildings from the remote server.
     *
     * @param job The job configuration blueprint.
     * @return The [JobResult].
     */
    suspend fun executeSyncBuildings(job: Job): JobResult

    /**
     * Attempts to fetch a building from the remote server.
     *
     * @param job The job configuration blueprint.
     * @return The [JobResult].
     */
    suspend fun executeSyncBuilding(job: Job): JobResult

    /**
     * Inserts a new building or updates an existing one.
     *
     * @param building The [Building] to be inserted or updated.
     */
    suspend fun upsertBuilding(building: Building)

    /**
     * Executes the background job to upsert a building, handling synchronization with the remote server.
     *
     * @param job The persistent background [Job] responsible for the upsert execution.
     * @return The [JobResult].
     */
    suspend fun executeUpsertBuilding(job: Job): JobResult
}
