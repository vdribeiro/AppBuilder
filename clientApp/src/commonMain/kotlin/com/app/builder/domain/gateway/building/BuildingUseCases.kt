package com.app.builder.domain.gateway.building

import kotlin.uuid.Uuid
import kotlinx.coroutines.flow.Flow
import com.app.builder.domain.Building

/** Defines the business workflows for buildings. */
interface BuildingUseCases {

    /**
     * Observes the complete list of buildings.
     *
     * @return [Flow] emitting the current list of [Building] objects.
     */
    fun observeBuildings(): Flow<List<Building>>

    /**
     * Observes a specific application building by its unique identifier.
     *
     * @param uuid The unique identifier of the building to observe.
     * @return [Flow] emitting the [Building] if found, or null if it does not exist.
     */
    fun observeBuilding(uuid: Uuid): Flow<Building?>

    /**
     * Inserts a new building or updates an existing one.
     *
     * @param building The [Building] to be inserted or updated.
     */
    suspend fun upsertBuilding(building: Building)
}
