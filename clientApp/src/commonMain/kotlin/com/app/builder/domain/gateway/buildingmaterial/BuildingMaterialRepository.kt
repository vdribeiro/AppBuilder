package com.app.builder.domain.gateway.buildingmaterial

import kotlin.uuid.Uuid
import com.app.builder.domain.BuildingMaterial
import com.app.builder.domain.scheduler.Job
import com.app.builder.domain.scheduler.JobResult

/** Defines the repository for building materials. */
interface BuildingMaterialRepository {

    /** Enqueues building materials synchronization. */
    suspend fun syncBuildingMaterials()

    /**
     * Enqueues a building material synchronization.
     *
     * @param uuid The unique identifier of the building material to sync.
     */
    suspend fun syncBuildingMaterial(uuid: Uuid)

    /**
     * Attempts to fetch the latest building materials from the remote server.
     *
     * @param job The job configuration blueprint.
     * @return The [JobResult].
     */
    suspend fun executeSyncBuildingMaterials(job: Job): JobResult

    /**
     * Attempts to fetch a building material from the remote server.
     *
     * @param job The job configuration blueprint.
     * @return The [JobResult].
     */
    suspend fun executeSyncBuildingMaterial(job: Job): JobResult

    /**
     * Inserts a new building material or updates an existing one.
     *
     * @param buildingMaterial The [BuildingMaterial] to be inserted or updated.
     */
    suspend fun upsertBuildingMaterial(buildingMaterial: BuildingMaterial)

    /**
     * Executes the background job to upsert a building material, handling synchronization with the remote server.
     *
     * @param job The persistent background [Job] responsible for the upsert execution.
     * @return The [JobResult].
     */
    suspend fun executeUpsertBuildingMaterial(job: Job): JobResult
}
