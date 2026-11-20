package com.app.builder.domain.gateway.equipmentuser

import kotlin.uuid.Uuid
import com.app.builder.domain.EquipmentUser
import com.app.builder.domain.scheduler.Job
import com.app.builder.domain.scheduler.JobResult

/** Defines the repository for equipment users. */
interface EquipmentUserRepository {

    /** Enqueues equipment users synchronization. */
    suspend fun syncEquipmentUsers()

    /**
     * Enqueues an equipment user synchronization.
     *
     * @param uuid The unique identifier of the equipment user to sync.
     */
    suspend fun syncEquipmentUser(uuid: Uuid)

    /**
     * Attempts to fetch the latest equipment users from the remote server.
     *
     * @param job The job configuration blueprint.
     * @return The [JobResult].
     */
    suspend fun executeSyncEquipmentUsers(job: Job): JobResult

    /**
     * Attempts to fetch an equipment user from the remote server.
     *
     * @param job The job configuration blueprint.
     * @return The [JobResult].
     */
    suspend fun executeSyncEquipmentUser(job: Job): JobResult

    /**
     * Inserts a new equipment user or updates an existing one.
     *
     * @param equipmentUser The [EquipmentUser] to be inserted or updated.
     */
    suspend fun upsertEquipmentUser(equipmentUser: EquipmentUser)

    /**
     * Executes the background job to upsert an equipment user, handling synchronization with the remote server.
     *
     * @param job The persistent background [Job] responsible for the upsert execution.
     * @return The [JobResult].
     */
    suspend fun executeUpsertEquipmentUser(job: Job): JobResult
}
