package com.app.builder.domain.gateway.config

import com.app.builder.domain.scheduler.Job
import com.app.builder.domain.scheduler.JobResult

/** Feature flags and configs repository. */
interface ConfigRepository {

    /** Restores persisted config records from disk. */
    suspend fun load()

    /** Enqueues a feature flags synchronization. */
    suspend fun syncFeatureFlags()

    /** Enqueues a remote configs synchronization. */
    suspend fun syncConfigs()

    /**
     * Synchronizes the feature flags from the remote config server.
     *
     * @param job The job configuration blueprint.
     * @return The [JobResult].
     */
    suspend fun executeSyncFeatureFlags(job: Job): JobResult

    /**
     * Synchronizes the remote configs from the remote config server.
     *
     * @param job The job configuration blueprint.
     * @return The [JobResult].
     */
    suspend fun executeSyncConfigs(job: Job): JobResult
}
