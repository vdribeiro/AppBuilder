package com.app.builder.domain.gateway.equipment

import com.app.builder.data.database.EquipmentSchema
import com.app.builder.domain.Equipment

/**
 * Maps an [EquipmentSchema] entity to an [Equipment] domain model.
 *
 * @return The corresponding [Equipment] instance.
 */
fun EquipmentSchema.toEquipment(): Equipment = Equipment(
    uuid = uuid,
    modifiedAt = modifiedAt,
    deletedAt = deletedAt,
    name = name,
    description = description,
    status = status
)

/**
 * Maps an [Equipment] domain model to an [EquipmentSchema] entity.
 *
 * @return The corresponding [EquipmentSchema] instance.
 */
fun Equipment.toEquipmentSchema(): EquipmentSchema = EquipmentSchema(
    uuid = uuid,
    modifiedAt = modifiedAt,
    deletedAt = deletedAt,
    name = name,
    description = description,
    status = status
)
