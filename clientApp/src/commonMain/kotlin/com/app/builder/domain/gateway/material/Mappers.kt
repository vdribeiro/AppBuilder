package com.app.builder.domain.gateway.material

import com.app.builder.data.database.MaterialSchema
import com.app.builder.domain.Material

/**
 * Maps an [MaterialSchema] entity to an [Material] domain model.
 *
 * @return The corresponding [Material] instance.
 */
fun MaterialSchema.toMaterial(): Material = Material(
    uuid = uuid,
    modifiedAt = modifiedAt,
    deletedAt = deletedAt,
    name = name,
    description = description
)

/**
 * Maps an [Material] domain model to an [MaterialSchema] entity.
 *
 * @return The corresponding [MaterialSchema] instance.
 */
fun Material.toMaterialSchema(): MaterialSchema = MaterialSchema(
    uuid = uuid,
    modifiedAt = modifiedAt,
    deletedAt = deletedAt,
    name = name,
    description = description
)
