package com.app.builder.domain.gateway.material

import kotlin.uuid.Uuid
import com.app.builder.domain.Material
import com.app.builder.domain.scheduler.Job
import com.app.builder.domain.scheduler.JobResult

/** Defines the repository for materials. */
interface MaterialRepository {

    /** Enqueues materials synchronization. */
    suspend fun syncMaterials()

    /**
     * Enqueues a material synchronization.
     *
     * @param uuid The unique identifier of the material to sync.
     */
    suspend fun syncMaterial(uuid: Uuid)

    /**
     * Attempts to fetch the latest materials from the remote server.
     *
     * @param job The job configuration blueprint.
     * @return The [JobResult].
     */
    suspend fun executeSyncMaterials(job: Job): JobResult

    /**
     * Attempts to fetch a material from the remote server.
     *
     * @param job The job configuration blueprint.
     * @return The [JobResult].
     */
    suspend fun executeSyncMaterial(job: Job): JobResult

    /**
     * Inserts a new material or updates an existing one.
     *
     * @param material The [Material] to be inserted or updated.
     */
    suspend fun upsertMaterial(material: Material)

    /**
     * Executes the background job to upsert a material, handling synchronization with the remote server.
     *
     * @param job The persistent background [Job] responsible for the upsert execution.
     * @return The [JobResult].
     */
    suspend fun executeUpsertMaterial(job: Job): JobResult
}
