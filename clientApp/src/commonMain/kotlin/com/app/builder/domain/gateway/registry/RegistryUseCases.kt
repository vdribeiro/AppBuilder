package com.app.builder.domain.gateway.registry

import kotlin.uuid.Uuid
import kotlinx.coroutines.flow.Flow
import com.app.builder.domain.Registry

/** Defines the business workflows for registries. */
interface RegistryUseCases {

    /**
     * Observes the complete list of registries.
     *
     * @return [Flow] emitting the current list of [Registry] objects.
     */
    fun observeRegistries(): Flow<List<Registry>>

    /**
     * Observes a specific registry by its unique identifier.
     *
     * @param uuid The unique identifier of the registry to observe.
     * @return [Flow] emitting the [Registry] if found, or null if it does not exist.
     */
    fun observeRegistry(uuid: Uuid): Flow<Registry?>
}
