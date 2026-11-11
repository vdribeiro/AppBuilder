package com.app.builder.domain.usecase.building

import kotlin.time.Instant
import kotlin.uuid.Uuid
import com.app.builder.core.config.ServerConfigs
import com.app.builder.domain.Building
import com.app.builder.domain.usecase.Outcome

/** Defines the business workflows for managing buildings. */
interface BuildingUseCases {

    /**
     * Gets a page of buildings updated after the given timestamp, newest first.
     *
     * @param lastSyncUtc The fixed lower bound below which results are already synced.
     * @param cursorUtc The `modifiedAt` of the last building consumed in the previous page, for descending cursor continuation.
     * @param cursorUuid The UUID of the last building consumed in the previous page, tie-breaking [cursorUtc].
     * @param pageSize The maximum number of entries to return, clamped to [ServerConfigs.maxPageSize].
     * @return [Outcome.Success] with the [Building] objects, or [Outcome.Error] when the operation failed.
     */
    suspend fun getBuildings(
        lastSyncUtc: Instant,
        cursorUtc: Instant?,
        cursorUuid: Uuid?,
        pageSize: Int
    ): Outcome<List<Building>>

    /**
     * Gets a single building by UUID.
     *
     * @param uuid The building's unique identifier.
     * @return [Outcome.Success] with the [Building], [Outcome.NotFound] when it does not exist, or [Outcome.Error] when the operation failed.
     */
    suspend fun getBuilding(uuid: Uuid): Outcome<Building>

    /**
     * Inserts a new building or updates an existing one.
     *
     * @param building The [Building] to be inserted or updated.
     * @return [Outcome.Success] when persisted, or [Outcome.Error] when the operation failed.
     */
    suspend fun upsertBuilding(building: Building): Outcome<Unit>
}
