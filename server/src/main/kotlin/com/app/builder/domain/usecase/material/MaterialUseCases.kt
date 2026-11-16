package com.app.builder.domain.usecase.material

import kotlin.time.Instant
import kotlin.uuid.Uuid
import com.app.builder.core.config.ServerConfigs
import com.app.builder.domain.Material
import com.app.builder.domain.usecase.Outcome

/** Defines the business workflows for managing materials. */
interface MaterialUseCases {

    /**
     * Gets a page of materials updated after the given timestamp, newest first.
     *
     * @param lastSyncUtc The fixed lower bound below which results are already synced.
     * @param cursorUtc The `modifiedAt` of the last material consumed in the previous page, for descending cursor continuation.
     * @param cursorUuid The UUID of the last material consumed in the previous page, tie-breaking [cursorUtc].
     * @param pageSize The maximum number of entries to return, clamped to [ServerConfigs.maxPageSize].
     * @return [Outcome.Success] with the [Material] objects, or [Outcome.Error] when the operation failed.
     */
    suspend fun getMaterials(
        lastSyncUtc: Instant,
        cursorUtc: Instant?,
        cursorUuid: Uuid?,
        pageSize: Int
    ): Outcome<List<Material>>

    /**
     * Gets a single material by UUID.
     *
     * @param uuid The material's unique identifier.
     * @return [Outcome.Success] with the [Material], [Outcome.NotFound] when it does not exist, or [Outcome.Error] when the operation failed.
     */
    suspend fun getMaterial(uuid: Uuid): Outcome<Material>

    /**
     * Inserts a new material or updates an existing one.
     *
     * @param material The [Material] to be inserted or updated.
     * @return [Outcome.Success] when persisted, or [Outcome.Error] when the operation failed.
     */
    suspend fun upsertMaterial(material: Material): Outcome<Unit>
}
