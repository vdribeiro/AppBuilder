package com.app.builder.domain.gateway.equipmentuser

import com.app.builder.data.database.EquipmentUserSchema
import com.app.builder.domain.EquipmentUser

/**
 * Maps a [EquipmentUserSchema] entity to a [EquipmentUser] domain model.
 *
 * @return The corresponding [EquipmentUser] instance.
 */
fun EquipmentUserSchema.toEquipmentUser(): EquipmentUser = EquipmentUser(
    modifiedAt = modifiedAt,
    deletedAt = deletedAt,
    equipmentUuid = equipmentUuid,
    userUuid = userUuid
)

/**
 * Maps a [EquipmentUser] domain model to a [EquipmentUserSchema] entity.
 *
 * @return The corresponding [EquipmentUserSchema] instance.
 */
fun EquipmentUser.toEquipmentUserSchema(): EquipmentUserSchema = EquipmentUserSchema(
    uuid = uuid,
    modifiedAt = modifiedAt,
    deletedAt = deletedAt,
    equipmentUuid = equipmentUuid,
    userUuid = userUuid
)
