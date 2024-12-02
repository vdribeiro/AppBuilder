package com.app.builder.domain.usecase.registry

import kotlin.time.Instant
import kotlin.uuid.Uuid
import com.app.builder.core.config.ServerConfigs
import com.app.builder.domain.Registry

/** Defines the business workflows for managing the request registry. */
interface RegistryUseCases {

    /**
     * Gets a page of registry entries updated after the given timestamp, newest first.
     *
     * @param lastSyncUtc The fixed lower bound below which results are already synced.
     * @param cursorUtc The `requestUtc` of the last registry entry consumed in the previous page, for descending cursor continuation.
     * @param cursorUuid The `requestUuid` of the last registry entry consumed in the previous page, tie-breaking [cursorUtc].
     * @param pageSize The maximum number of entries to return, clamped to [ServerConfigs.maxPageSize].
     * @return A list of registry entries.
     */
    suspend fun getRegistries(lastSyncUtc: Instant, cursorUtc: Instant?, cursorUuid: Uuid?, pageSize: Int): List<Registry>

    /**
     * Persists a registry entry to the database.
     *
     * @param registry The [Registry] entry to save.
     * @return `true` if the registry entry was saved successfully, `false` otherwise or no rows were affected.
     */
    suspend fun upsertRegistry(registry: Registry): Boolean
}
