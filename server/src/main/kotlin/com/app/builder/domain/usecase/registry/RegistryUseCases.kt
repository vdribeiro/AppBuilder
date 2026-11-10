package com.app.builder.domain.usecase.registry

import kotlin.time.Instant
import kotlin.uuid.Uuid
import com.app.builder.core.config.ServerConfigs
import com.app.builder.domain.Registry
import com.app.builder.domain.usecase.Outcome

/** Defines the business workflows for managing the request registry. */
interface RegistryUseCases {

    /**
     * Gets a page of registry entries updated after the given timestamp, newest first.
     *
     * @param lastSyncUtc The fixed lower bound below which results are already synced.
     * @param cursorUtc The `requestUtc` of the last registry entry consumed in the previous page, for descending cursor continuation.
     * @param cursorUuid The `requestUuid` of the last registry entry consumed in the previous page, tie-breaking [cursorUtc].
     * @param pageSize The maximum number of entries to return, clamped to [ServerConfigs.maxPageSize].
     * @return [Outcome.Success] with the registry entries, or [Outcome.Error] when the operation failed.
     */
    suspend fun getRegistries(lastSyncUtc: Instant, cursorUtc: Instant?, cursorUuid: Uuid?, pageSize: Int): Outcome<List<Registry>>

    /**
     * Persists a registry entry to the database.
     *
     * @param registry The [Registry] entry to save.
     * @return [Outcome.Success] when saved, or [Outcome.Error] when the operation failed or no rows were affected.
     */
    suspend fun upsertRegistry(registry: Registry): Outcome<Unit>
}
