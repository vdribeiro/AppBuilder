package com.app.builder.domain.gateway.buildingequipment

import kotlin.uuid.Uuid
import com.app.builder.domain.BuildingEquipment
import com.app.builder.domain.scheduler.Job
import com.app.builder.domain.scheduler.JobResult

/** Defines the repository for building equipments. */
interface BuildingEquipmentRepository {

    /** Enqueues building equipments synchronization. */
    suspend fun syncBuildingEquipments()

    /**
     * Enqueues a building equipment synchronization.
     *
     * @param uuid The unique identifier of the building equipment to sync.
     */
    suspend fun syncBuildingEquipment(uuid: Uuid)

    /**
     * Attempts to fetch the latest building equipments from the remote server.
     *
     * @param job The job configuration blueprint.
     * @return The [JobResult].
     */
    suspend fun executeSyncBuildingEquipments(job: Job): JobResult

    /**
     * Attempts to fetch a building equipment from the remote server.
     *
     * @param job The job configuration blueprint.
     * @return The [JobResult].
     */
    suspend fun executeSyncBuildingEquipment(job: Job): JobResult

    /**
     * Inserts a new building equipment or updates an existing one.
     *
     * @param buildingEquipment The [BuildingEquipment] to be inserted or updated.
     */
    suspend fun upsertBuildingEquipment(buildingEquipment: BuildingEquipment)

    /**
     * Executes the background job to upsert a building equipment, handling synchronization with the remote server.
     *
     * @param job The persistent background [Job] responsible for the upsert execution.
     * @return The [JobResult].
     */
    suspend fun executeUpsertBuildingEquipment(job: Job): JobResult
}
