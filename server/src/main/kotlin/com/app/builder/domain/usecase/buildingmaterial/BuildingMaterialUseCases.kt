package com.app.builder.domain.usecase.buildingmaterial

import kotlin.time.Instant
import kotlin.uuid.Uuid
import com.app.builder.core.config.ServerConfigs
import com.app.builder.domain.BuildingMaterial
import com.app.builder.domain.usecase.Outcome

/** Defines the business workflows for managing building materials. */
interface BuildingMaterialUseCases {

    /**
     * Gets a page of building materials updated after the given timestamp, newest first.
     *
     * @param lastSyncUtc The fixed lower bound below which results are already synced.
     * @param cursorUtc The `modifiedAt` of the last building material consumed in the previous page, for descending cursor continuation.
     * @param cursorUuid The UUID of the last building material consumed in the previous page, tie-breaking [cursorUtc].
     * @param pageSize The maximum number of entries to return, clamped to [ServerConfigs.maxPageSize].
     * @return [Outcome.Success] with the [BuildingMaterial] objects, or [Outcome.Error] when the operation failed.
     */
    suspend fun getBuildingMaterials(
        lastSyncUtc: Instant,
        cursorUtc: Instant?,
        cursorUuid: Uuid?,
        pageSize: Int
    ): Outcome<List<BuildingMaterial>>

    /**
     * Gets a single building material by UUID.
     *
     * @param uuid The building material's unique identifier.
     * @return [Outcome.Success] with the [BuildingMaterial], [Outcome.NotFound] when it does not exist, or [Outcome.Error] when the operation failed.
     */
    suspend fun getBuildingMaterial(uuid: Uuid): Outcome<BuildingMaterial>

    /**
     * Inserts a new building material or updates an existing one.
     *
     * @param buildingMaterial The [BuildingMaterial] to be inserted or updated.
     * @return [Outcome.Success] when persisted, or [Outcome.Error] when the operation failed.
     */
    suspend fun upsertBuildingMaterial(buildingMaterial: BuildingMaterial): Outcome<Unit>
}
