package com.app.builder.domain.gateway.buildingequipment

import com.app.builder.data.database.BuildingEquipmentSchema
import com.app.builder.domain.BuildingEquipment

/**
 * Maps a [BuildingEquipmentSchema] entity to a [BuildingEquipment] domain model.
 *
 * @return The corresponding [BuildingEquipment] instance.
 */
fun BuildingEquipmentSchema.toBuildingEquipment(): BuildingEquipment = BuildingEquipment(
    modifiedAt = modifiedAt,
    deletedAt = deletedAt,
    buildingUuid = buildingUuid,
    equipmentUuid = equipmentUuid
)

/**
 * Maps a [BuildingEquipment] domain model to a [BuildingEquipmentSchema] entity.
 *
 * @return The corresponding [BuildingEquipmentSchema] instance.
 */
fun BuildingEquipment.toBuildingEquipmentSchema(): BuildingEquipmentSchema = BuildingEquipmentSchema(
    uuid = uuid,
    modifiedAt = modifiedAt,
    deletedAt = deletedAt,
    buildingUuid = buildingUuid,
    equipmentUuid = equipmentUuid
)
