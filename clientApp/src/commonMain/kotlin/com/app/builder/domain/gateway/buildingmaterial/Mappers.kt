package com.app.builder.domain.gateway.buildingmaterial

import com.app.builder.data.database.BuildingMaterialSchema
import com.app.builder.domain.Building
import com.app.builder.domain.BuildingMaterial
import com.app.builder.domain.Material
import database.GetBuildingsOfMaterial
import database.GetMaterialsInBuilding

/**
 * Maps a [BuildingMaterialSchema] entity to a [BuildingMaterial] domain model.
 *
 * @return The corresponding [BuildingMaterial] instance.
 */
fun BuildingMaterialSchema.toBuildingMaterial(): BuildingMaterial = BuildingMaterial(
    modifiedAt = modifiedAt,
    deletedAt = deletedAt,
    buildingUuid = buildingUuid,
    materialUuid = materialUuid,
    quantity = quantity
)

/**
 * Maps a [BuildingMaterial] domain model to a [BuildingMaterialSchema] entity.
 *
 * @return The corresponding [BuildingMaterialSchema] instance.
 */
fun BuildingMaterial.toBuildingMaterialSchema(): BuildingMaterialSchema = BuildingMaterialSchema(
    uuid = uuid,
    modifiedAt = modifiedAt,
    deletedAt = deletedAt,
    buildingUuid = buildingUuid,
    materialUuid = materialUuid,
    quantity = quantity
)

/**
 * Maps a [GetMaterialsInBuilding] row to a [MaterialInBuilding].
 *
 * @return The corresponding [MaterialInBuilding] instance.
 */
fun GetMaterialsInBuilding.toMaterialInBuilding(): MaterialInBuilding = MaterialInBuilding(
    material = Material(
        uuid = uuid,
        modifiedAt = modifiedAt,
        deletedAt = deletedAt,
        name = name,
        description = description
    ),
    quantity = quantity
)

/**
 * Maps a [GetBuildingsOfMaterial] row to a [BuildingOfMaterial].
 *
 * @return The corresponding [BuildingOfMaterial] instance.
 */
fun GetBuildingsOfMaterial.toBuildingOfMaterial(): BuildingOfMaterial = BuildingOfMaterial(
    building = Building(
        uuid = uuid,
        modifiedAt = modifiedAt,
        deletedAt = deletedAt,
        name = name,
        description = description,
        address = address,
        latitude = latitude,
        longitude = longitude
    ),
    quantity = quantity
)
