package com.app.builder.domain

import kotlin.time.Instant
import kotlin.uuid.Uuid
import kotlinx.serialization.Serializable

/**
 * Material entity.
 *
 * @property uuid The unique, immutable identifier for this material.
 * @property modifiedAt The timestamp indicating when this material was last updated.
 * @property deletedAt The timestamp indicating when this material was last deleted.
 * @property name The name of the material.
 * @property code The code identifying the material.
 */
@Serializable
data class Material(
    val uuid: Uuid,
    val modifiedAt: Instant,
    val deletedAt: Instant?,
    val name: String,
    val code: String
) {
    /**
     * Enumerates the [Material] fields that map to a localized display label.
     *
     * @property translationKey The [Translation.key] used to look up the localized label for this field.
     */
    @Serializable
    enum class Property(val translationKey: String) {
        MODIFIED_AT(translationKey = "material_modified_at"),
        DELETED_AT(translationKey = "material_deleted_at"),
        NAME(translationKey = "material_name"),
        CODE(translationKey = "material_code")
    }
}
