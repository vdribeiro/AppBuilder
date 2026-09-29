package com.app.builder.domain.gateway.equipment

import kotlin.uuid.Uuid
import com.app.builder.domain.Equipment
import com.app.builder.domain.scheduler.Job
import com.app.builder.domain.scheduler.JobResult

/** Defines the repository for equipments. */
interface EquipmentRepository {

    /** Enqueues equipments synchronization. */
    suspend fun syncEquipments()

    /**
     * Enqueues a equipment synchronization.
     *
     * @param uuid The unique identifier of the equipment to sync.
     */
    suspend fun syncEquipment(uuid: Uuid)

    /**
     * Attempts to fetch the latest equipments from the remote server.
     *
     * @param job The job configuration blueprint.
     * @return The [JobResult].
     */
    suspend fun executeSyncEquipments(job: Job): JobResult

    /**
     * Attempts to fetch a equipment from the remote server.
     *
     * @param job The job configuration blueprint.
     * @return The [JobResult].
     */
    suspend fun executeSyncEquipment(job: Job): JobResult

    /**
     * Inserts a new equipment or updates an existing one.
     *
     * @param equipment The [Equipment] to be inserted or updated.
     */
    suspend fun upsertEquipment(equipment: Equipment)

    /**
     * Executes the background job to upsert a equipment, handling synchronization with the remote server.
     *
     * @param job The persistent background [Job] responsible for the upsert execution.
     * @return The [JobResult].
     */
    suspend fun executeUpsertEquipment(job: Job): JobResult
}
