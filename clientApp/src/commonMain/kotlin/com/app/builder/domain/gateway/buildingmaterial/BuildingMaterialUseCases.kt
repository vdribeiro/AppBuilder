package com.app.builder.domain.gateway.buildingmaterial

import kotlin.uuid.Uuid
import kotlinx.coroutines.flow.Flow
import com.app.builder.domain.BuildingMaterial

/** Defines the business workflows for building materials. */
interface BuildingMaterialUseCases {

    /**
     * Observes the materials stored in a building with their quantities, leaving out deleted materials and deleted building materials.
     *
     * @param buildingUuid The unique identifier of the building.
     * @return [Flow] emitting the current list of [MaterialInBuilding] of the building.
     */
    fun observeMaterialsInBuilding(buildingUuid: Uuid): Flow<List<MaterialInBuilding>>

    /**
     * Observes the buildings storing a material with their quantities, leaving out deleted buildings and deleted building materials.
     *
     * @param materialUuid The unique identifier of the material.
     * @return [Flow] emitting the current list of [BuildingOfMaterial] of the material.
     */
    fun observeBuildingsOfMaterial(materialUuid: Uuid): Flow<List<BuildingOfMaterial>>

    /**
     * Inserts a new building material or updates an existing one.
     *
     * @param buildingMaterial The [BuildingMaterial] to be inserted or updated.
     */
    suspend fun upsertBuildingMaterial(buildingMaterial: BuildingMaterial)
}
