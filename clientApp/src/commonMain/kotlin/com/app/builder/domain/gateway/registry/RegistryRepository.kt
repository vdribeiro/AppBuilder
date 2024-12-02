package com.app.builder.domain.gateway.registry

import com.app.builder.domain.scheduler.Job
import com.app.builder.domain.scheduler.JobResult

/** Defines the repository for registries. */
interface RegistryRepository {

    /** Enqueues registries synchronization. */
    suspend fun syncRegistries()

    /**
     * Attempts to fetch the latest registries from the remote server.
     *
     * @param job The job configuration blueprint.
     * @return The [JobResult].
     */
    suspend fun executeSyncRegistries(job: Job): JobResult
}
