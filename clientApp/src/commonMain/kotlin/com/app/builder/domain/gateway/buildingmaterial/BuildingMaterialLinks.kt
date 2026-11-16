package com.app.builder.domain.gateway.buildingmaterial

import com.app.builder.domain.Building
import com.app.builder.domain.Material

/**
 * A material stored in a building.
 *
 * @property material The stored material.
 * @property quantity The quantity of the material stored in the building.
 */
data class MaterialInBuilding(
    val material: Material,
    val quantity: Int
)

/**
 * A building storing a material.
 *
 * @property building The building storing the material.
 * @property quantity The quantity of the material stored in the building.
 */
data class BuildingOfMaterial(
    val building: Building,
    val quantity: Int
)
