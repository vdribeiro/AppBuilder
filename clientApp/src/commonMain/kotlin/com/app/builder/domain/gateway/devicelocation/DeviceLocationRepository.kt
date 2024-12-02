package com.app.builder.domain.gateway.devicelocation

import com.app.builder.domain.scheduler.Job
import com.app.builder.domain.scheduler.JobResult

/** Defines the repository for device location. */
interface DeviceLocationRepository {

    /** Enqueues device locations synchronization. */
    suspend fun syncDeviceLocations()

    /**
     * Attempts to fetch the latest device locations from the remote server.
     *
     * @param job The job configuration blueprint.
     * @return The [JobResult].
     */
    suspend fun executeSyncDeviceLocations(job: Job): JobResult
}
