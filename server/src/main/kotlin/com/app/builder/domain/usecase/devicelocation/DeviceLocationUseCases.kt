package com.app.builder.domain.usecase.devicelocation

import kotlin.time.Instant
import kotlin.uuid.Uuid
import com.app.builder.core.config.ServerConfigs
import com.app.builder.domain.DeviceLocation

/** Defines the business workflows for device location. */
interface DeviceLocationUseCases {

    /**
     * Gets a page of device locations belonging to a user, updated after the given timestamp, newest first.
     *
     * @param userUuid The unique identifier of the user.
     * @param lastSyncUtc The fixed lower bound below which results are already synced.
     * @param cursorUtc The `deviceTime` of the last location consumed in the previous page, for descending cursor continuation.
     * @param cursorUuid The UUID of the last location consumed in the previous page, tie-breaking [cursorUtc].
     * @param pageSize The maximum number of entries to return, clamped to [ServerConfigs.maxPageSize].
     * @return The list of [DeviceLocation] objects.
     */
    suspend fun getDeviceLocations(userUuid: Uuid, lastSyncUtc: Instant, cursorUtc: Instant?, cursorUuid: Uuid?, pageSize: Int): List<DeviceLocation>

    /**
     * Records a device location fix reported by a user's device.
     *
     * @param userUuid The owning user's UUID.
     * @param deviceLocation The reported location fix.
     * @return `true` if the fix was persisted successfully, `false` otherwise.
     */
    suspend fun upsertDeviceLocation(userUuid: Uuid, deviceLocation: DeviceLocation): Boolean
}
