package com.app.builder.domain.gateway.registry

import kotlin.uuid.Uuid
import kotlinx.coroutines.flow.Flow

/** Defines the business workflows for registries. */
interface RegistryUseCases {

    /**
     * Observes the complete list of registries, each with the name of its user when the account can read users.
     *
     * @return [Flow] emitting the current list of [RegistryEntry] objects.
     */
    fun observeRegistries(): Flow<List<RegistryEntry>>

    /**
     * Observes a specific registry by its unique identifier, with the name of its user when the account can read users.
     *
     * @param uuid The unique identifier of the registry to observe.
     * @return [Flow] emitting the [RegistryEntry] if found, or null if it does not exist.
     */
    fun observeRegistry(uuid: Uuid): Flow<RegistryEntry?>
}
