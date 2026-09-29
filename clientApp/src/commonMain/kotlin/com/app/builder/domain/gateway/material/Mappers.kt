package com.app.builder.domain.gateway.material

import com.app.builder.data.database.MaterialSchema
import com.app.builder.domain.Material

/**
 * Maps a [MaterialSchema] entity to a [Material] domain model.
 *
 * @return The corresponding [Material] instance.
 */
fun MaterialSchema.toMaterial(): Material = Material(
    uuid = uuid,
    modifiedAt = modifiedAt,
    deletedAt = deletedAt,
    name = name,
    code = code
)

/**
 * Maps a [Material] domain model to a [MaterialSchema] entity.
 *
 * @return The corresponding [MaterialSchema] instance.
 */
fun Material.toMaterialSchema(): MaterialSchema = MaterialSchema(
    uuid = uuid,
    modifiedAt = modifiedAt,
    deletedAt = deletedAt,
    name = name,
    code = code
)
