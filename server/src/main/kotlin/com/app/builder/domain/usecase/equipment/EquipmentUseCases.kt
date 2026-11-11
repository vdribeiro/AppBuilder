package com.app.builder.domain.usecase.equipment

import kotlin.time.Instant
import kotlin.uuid.Uuid
import com.app.builder.core.config.ServerConfigs
import com.app.builder.domain.Equipment
import com.app.builder.domain.usecase.Outcome

/** Defines the business workflows for managing equipments. */
interface EquipmentUseCases {

    /**
     * Gets a page of equipments updated after the given timestamp, newest first.
     *
     * @param lastSyncUtc The fixed lower bound below which results are already synced.
     * @param cursorUtc The `modifiedAt` of the last equipment consumed in the previous page, for descending cursor continuation.
     * @param cursorUuid The UUID of the last equipment consumed in the previous page, tie-breaking [cursorUtc].
     * @param pageSize The maximum number of entries to return, clamped to [ServerConfigs.maxPageSize].
     * @return [Outcome.Success] with the [Equipment] objects, or [Outcome.Error] when the operation failed.
     */
    suspend fun getEquipments(
        lastSyncUtc: Instant,
        cursorUtc: Instant?,
        cursorUuid: Uuid?,
        pageSize: Int
    ): Outcome<List<Equipment>>

    /**
     * Gets a single equipment by UUID.
     *
     * @param uuid The equipment's unique identifier.
     * @return [Outcome.Success] with the [Equipment], [Outcome.NotFound] when it does not exist, or [Outcome.Error] when the operation failed.
     */
    suspend fun getEquipment(uuid: Uuid): Outcome<Equipment>

    /**
     * Inserts a new equipment or updates an existing one.
     *
     * @param equipment The [Equipment] to be inserted or updated.
     * @return [Outcome.Success] when persisted, or [Outcome.Error] when the operation failed.
     */
    suspend fun upsertEquipment(equipment: Equipment): Outcome<Unit>
}
