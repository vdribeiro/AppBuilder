package com.app.builder.domain.gateway.equipment

import com.app.builder.data.database.EquipmentSchema
import com.app.builder.domain.Equipment

/**
 * Maps a [EquipmentSchema] entity to a [Equipment] domain model.
 *
 * @return The corresponding [Equipment] instance.
 */
fun EquipmentSchema.toEquipment(): Equipment = Equipment(
    uuid = uuid,
    modifiedAt = modifiedAt,
    deletedAt = deletedAt,
    name = name,
    code = code
)

/**
 * Maps a [Equipment] domain model to a [EquipmentSchema] entity.
 *
 * @return The corresponding [EquipmentSchema] instance.
 */
fun Equipment.toEquipmentSchema(): EquipmentSchema = EquipmentSchema(
    uuid = uuid,
    modifiedAt = modifiedAt,
    deletedAt = deletedAt,
    name = name,
    code = code
)
