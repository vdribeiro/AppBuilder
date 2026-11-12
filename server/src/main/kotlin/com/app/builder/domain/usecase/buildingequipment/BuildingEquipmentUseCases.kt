package com.app.builder.domain.usecase.buildingequipment

import kotlin.time.Instant
import kotlin.uuid.Uuid
import com.app.builder.core.config.ServerConfigs
import com.app.builder.domain.BuildingEquipment
import com.app.builder.domain.usecase.Outcome

/** Defines the business workflows for managing building equipments. */
interface BuildingEquipmentUseCases {

    /**
     * Gets a page of building equipments updated after the given timestamp, newest first.
     *
     * @param lastSyncUtc The fixed lower bound below which results are already synced.
     * @param cursorUtc The `modifiedAt` of the last building equipment consumed in the previous page, for descending cursor continuation.
     * @param cursorUuid The UUID of the last building equipment consumed in the previous page, tie-breaking [cursorUtc].
     * @param pageSize The maximum number of entries to return, clamped to [ServerConfigs.maxPageSize].
     * @return [Outcome.Success] with the [BuildingEquipment] objects, or [Outcome.Error] when the operation failed.
     */
    suspend fun getBuildingEquipments(
        lastSyncUtc: Instant,
        cursorUtc: Instant?,
        cursorUuid: Uuid?,
        pageSize: Int
    ): Outcome<List<BuildingEquipment>>

    /**
     * Gets a single building equipment by UUID.
     *
     * @param uuid The building equipment's unique identifier.
     * @return [Outcome.Success] with the [BuildingEquipment], [Outcome.NotFound] when it does not exist, or [Outcome.Error] when the operation failed.
     */
    suspend fun getBuildingEquipment(uuid: Uuid): Outcome<BuildingEquipment>

    /**
     * Inserts a new building equipment or updates an existing one.
     *
     * @param buildingEquipment The [BuildingEquipment] to be inserted or updated.
     * @return [Outcome.Success] when persisted, or [Outcome.Error] when the operation failed.
     */
    suspend fun upsertBuildingEquipment(buildingEquipment: BuildingEquipment): Outcome<Unit>
}
