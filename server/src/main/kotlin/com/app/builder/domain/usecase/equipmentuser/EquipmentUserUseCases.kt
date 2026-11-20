package com.app.builder.domain.usecase.equipmentuser

import kotlin.time.Instant
import kotlin.uuid.Uuid
import com.app.builder.core.config.ServerConfigs
import com.app.builder.domain.EquipmentUser
import com.app.builder.domain.usecase.Outcome

/** Defines the business workflows for managing equipment users. */
interface EquipmentUserUseCases {

    /**
     * Gets a page of equipment users updated after the given timestamp, newest first.
     *
     * @param lastSyncUtc The fixed lower bound below which results are already synced.
     * @param cursorUtc The `modifiedAt` of the last equipment user consumed in the previous page, for descending cursor continuation.
     * @param cursorUuid The UUID of the last equipment user consumed in the previous page, tie-breaking [cursorUtc].
     * @param pageSize The maximum number of entries to return, clamped to [ServerConfigs.maxPageSize].
     * @return [Outcome.Success] with the [EquipmentUser] objects, or [Outcome.Error] when the operation failed.
     */
    suspend fun getEquipmentUsers(
        lastSyncUtc: Instant,
        cursorUtc: Instant?,
        cursorUuid: Uuid?,
        pageSize: Int
    ): Outcome<List<EquipmentUser>>

    /**
     * Gets a single equipment user by UUID.
     *
     * @param uuid The equipment user's unique identifier.
     * @return [Outcome.Success] with the [EquipmentUser], [Outcome.NotFound] when it does not exist, or [Outcome.Error] when the operation failed.
     */
    suspend fun getEquipmentUser(uuid: Uuid): Outcome<EquipmentUser>

    /**
     * Inserts a new equipment user or updates an existing one.
     *
     * @param equipmentUser The [EquipmentUser] to be inserted or updated.
     * @return [Outcome.Success] when persisted, or [Outcome.Error] when the operation failed.
     */
    suspend fun upsertEquipmentUser(equipmentUser: EquipmentUser): Outcome<Unit>
}
