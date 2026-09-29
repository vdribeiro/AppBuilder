package com.app.builder.domain.gateway.building

import com.app.builder.data.database.BuildingSchema
import com.app.builder.domain.Building

/**
 * Maps a [BuildingSchema] entity to a [Building] domain model.
 *
 * @return The corresponding [Building] instance.
 */
fun BuildingSchema.toBuilding(): Building = Building(
    uuid = uuid,
    modifiedAt = modifiedAt,
    deletedAt = deletedAt,
    name = name,
    code = code
)

/**
 * Maps a [Building] domain model to a [BuildingSchema] entity.
 *
 * @return The corresponding [BuildingSchema] instance.
 */
fun Building.toBuildingSchema(): BuildingSchema = BuildingSchema(
    uuid = uuid,
    modifiedAt = modifiedAt,
    deletedAt = deletedAt,
    name = name,
    code = code
)
