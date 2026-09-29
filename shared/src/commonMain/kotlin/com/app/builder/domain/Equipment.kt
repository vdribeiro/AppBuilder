package com.app.builder.domain

import kotlin.time.Instant
import kotlin.uuid.Uuid
import kotlinx.serialization.Serializable

/**
 * Equipment entity.
 *
 * @property uuid The unique, immutable identifier for this equipment.
 * @property modifiedAt The timestamp indicating when this equipment was last updated.
 * @property deletedAt The timestamp indicating when this equipment was last deleted.
 * @property name The name of the equipment.
 * @property code The code identifying the equipment.
 */
@Serializable
data class Equipment(
    val uuid: Uuid,
    val modifiedAt: Instant,
    val deletedAt: Instant?,
    val name: String,
    val code: String
) {
    /**
     * Enumerates the [Equipment] fields that map to a localized display label.
     *
     * @property translationKey The [Translation.key] used to look up the localized label for this field.
     */
    @Serializable
    enum class Property(val translationKey: String) {
        MODIFIED_AT(translationKey = "equipment_modified_at"),
        DELETED_AT(translationKey = "equipment_deleted_at"),
        NAME(translationKey = "equipment_name"),
        CODE(translationKey = "equipment_code")
    }
}
