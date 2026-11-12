package com.app.builder.domain.gateway.buildingequipment

import kotlin.uuid.Uuid
import kotlinx.coroutines.flow.Flow
import com.app.builder.domain.Building
import com.app.builder.domain.BuildingEquipment
import com.app.builder.domain.Equipment

/** Defines the business workflows for building equipments. */
interface BuildingEquipmentUseCases {

    /**
     * Observes the equipments linked to a building, leaving out deleted equipments and deleted building equipments.
     *
     * @param buildingUuid The unique identifier of the building.
     * @return [Flow] emitting the current list of [Equipment] linked to the building.
     */
    fun observeEquipmentsInBuilding(buildingUuid: Uuid): Flow<List<Equipment>>

    /**
     * Observes the buildings linked to an equipment, leaving out deleted buildings and deleted building equipments.
     *
     * @param equipmentUuid The unique identifier of the equipment.
     * @return [Flow] emitting the current list of [Building] linked to the equipment.
     */
    fun observeBuildingsOfEquipment(equipmentUuid: Uuid): Flow<List<Building>>

    /**
     * Inserts a new building equipment or updates an existing one.
     *
     * @param buildingEquipment The [BuildingEquipment] to be inserted or updated.
     */
    suspend fun upsertBuildingEquipment(buildingEquipment: BuildingEquipment)
}
