package com.app.builder.domain.gateway.devicelocation

import io.ktor.client.HttpClient
import com.app.builder.domain.DeviceLocation
import com.app.builder.domain.User
import com.app.builder.domain.scheduler.Job
import com.app.builder.domain.scheduler.JobResult
import com.app.builder.domain.scheduler.Scheduler
import database.AppDatabase

/**
 * Gateway implementation for Device Location Repository.
 *
 * @property user The current user.
 * @property database The SQLite database instance for device location data.
 * @property httpClient The HTTP client used for network operations.
 * @property scheduler The job scheduler for background tasks.
 */
class DeviceLocationGateway(
    private val user: User,
    private val database: AppDatabase,
    private val httpClient: HttpClient,
    private val scheduler: Scheduler
): DeviceLocationRepository {

    /** Query interface for the device location table. */
    private val deviceLocationDao = database.deviceLocationQueries

    /**
     * Inserts or updates a device location in the local database.
     *
     * @param deviceLocation The [DeviceLocation] domain model containing the details to save.
     * @return `true` if the device location was successfully upserted; `false` if no rows were affected.
     */
    private suspend fun upsertDeviceLocationInDatabase(deviceLocation: DeviceLocation): Boolean =
        deviceLocationDao.upsertDeviceLocation(DeviceLocation = deviceLocation.toDeviceLocationSchema()) > 0

    override suspend fun syncDeviceLocations() {
        TODO("Not yet implemented")
    }

    override suspend fun executeSyncDeviceLocations(job: Job): JobResult {
        TODO("Not yet implemented")
    }

    companion object {
        private const val TAG = "DeviceLocationGateway"
    }
}
