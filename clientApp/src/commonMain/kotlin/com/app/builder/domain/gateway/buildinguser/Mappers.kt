package com.app.builder.domain.gateway.buildinguser

import com.app.builder.data.database.BuildingUserSchema
import com.app.builder.domain.BuildingUser

/**
 * Maps a [BuildingUserSchema] entity to a [BuildingUser] domain model.
 *
 * @return The corresponding [BuildingUser] instance.
 */
fun BuildingUserSchema.toBuildingUser(): BuildingUser = BuildingUser(
    modifiedAt = modifiedAt,
    deletedAt = deletedAt,
    buildingUuid = buildingUuid,
    userUuid = userUuid
)

/**
 * Maps a [BuildingUser] domain model to a [BuildingUserSchema] entity.
 *
 * @return The corresponding [BuildingUserSchema] instance.
 */
fun BuildingUser.toBuildingUserSchema(): BuildingUserSchema = BuildingUserSchema(
    uuid = uuid,
    modifiedAt = modifiedAt,
    deletedAt = deletedAt,
    buildingUuid = buildingUuid,
    userUuid = userUuid
)
