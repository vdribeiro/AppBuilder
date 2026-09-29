package com.app.builder.domain.gateway.equipment

import kotlin.uuid.Uuid
import kotlinx.coroutines.flow.Flow
import com.app.builder.domain.Equipment

/** Defines the business workflows for equipments. */
interface EquipmentUseCases {

    /**
     * Observes the complete list of equipments.
     *
     * @return [Flow] emitting the current list of [Equipment] objects.
     */
    fun observeEquipments(): Flow<List<Equipment>>

    /**
     * Observes a specific application equipment by its unique identifier.
     *
     * @param uuid The unique identifier of the equipment to observe.
     * @return [Flow] emitting the [Equipment] if found, or null if it does not exist.
     */
    fun observeEquipment(uuid: Uuid): Flow<Equipment?>

    /**
     * Inserts a new equipment or updates an existing one.
     *
     * @param equipment The [Equipment] to be inserted or updated.
     */
    suspend fun upsertEquipment(equipment: Equipment)
}
