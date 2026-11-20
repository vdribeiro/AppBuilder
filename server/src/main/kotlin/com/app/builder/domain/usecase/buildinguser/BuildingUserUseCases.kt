package com.app.builder.domain.usecase.buildinguser

import kotlin.time.Instant
import kotlin.uuid.Uuid
import com.app.builder.core.config.ServerConfigs
import com.app.builder.domain.BuildingUser
import com.app.builder.domain.usecase.Outcome

/** Defines the business workflows for managing building users. */
interface BuildingUserUseCases {

    /**
     * Gets a page of building users updated after the given timestamp, newest first.
     *
     * @param lastSyncUtc The fixed lower bound below which results are already synced.
     * @param cursorUtc The `modifiedAt` of the last building user consumed in the previous page, for descending cursor continuation.
     * @param cursorUuid The UUID of the last building user consumed in the previous page, tie-breaking [cursorUtc].
     * @param pageSize The maximum number of entries to return, clamped to [ServerConfigs.maxPageSize].
     * @return [Outcome.Success] with the [BuildingUser] objects, or [Outcome.Error] when the operation failed.
     */
    suspend fun getBuildingUsers(
        lastSyncUtc: Instant,
        cursorUtc: Instant?,
        cursorUuid: Uuid?,
        pageSize: Int
    ): Outcome<List<BuildingUser>>

    /**
     * Gets a single building user by UUID.
     *
     * @param uuid The building user's unique identifier.
     * @return [Outcome.Success] with the [BuildingUser], [Outcome.NotFound] when it does not exist, or [Outcome.Error] when the operation failed.
     */
    suspend fun getBuildingUser(uuid: Uuid): Outcome<BuildingUser>

    /**
     * Inserts a new building user or updates an existing one.
     *
     * @param buildingUser The [BuildingUser] to be inserted or updated.
     * @return [Outcome.Success] when persisted, or [Outcome.Error] when the operation failed.
     */
    suspend fun upsertBuildingUser(buildingUser: BuildingUser): Outcome<Unit>
}
