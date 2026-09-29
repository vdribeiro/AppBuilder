package com.app.builder.domain

import kotlin.time.Instant
import kotlin.uuid.Uuid
import kotlinx.serialization.Serializable

/**
 * Building entity.
 *
 * @property uuid The unique, immutable identifier for this building.
 * @property modifiedAt The timestamp indicating when this building was last updated.
 * @property deletedAt The timestamp indicating when this building was last deleted.
 * @property name The name of the building.
 * @property code The code identifying the building.
 */
@Serializable
data class Building(
    val uuid: Uuid,
    val modifiedAt: Instant,
    val deletedAt: Instant?,
    val name: String,
    val code: String
) {
    /**
     * Enumerates the [Building] fields that map to a localized display label.
     *
     * @property translationKey The [Translation.key] used to look up the localized label for this field.
     */
    @Serializable
    enum class Property(val translationKey: String) {
        MODIFIED_AT(translationKey = "building_modified_at"),
        DELETED_AT(translationKey = "building_deleted_at"),
        NAME(translationKey = "building_name"),
        CODE(translationKey = "building_code")
    }
}
