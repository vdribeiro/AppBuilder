package com.app.builder.domain

import kotlin.time.Instant
import kotlin.uuid.Uuid
import kotlinx.serialization.Serializable
import com.app.builder.core.locale.now
import com.app.builder.core.security.uuid

/**
 * Building entity.
 *
 * @property uuid The unique, immutable identifier for this building.
 * @property modifiedAt The timestamp indicating when this building was last updated.
 * @property deletedAt The timestamp indicating when this building was last deleted.
 * @property name The name of the building.
 * @property description The descriptive content detailing the building.
 * @property address The postal address of the building.
 * @property latitude The latitude of the building in degrees, or `null` when unknown.
 * @property longitude The longitude of the building in degrees, or `null` when unknown.
 */
@Serializable
data class Building(
    val uuid: Uuid = uuid(),
    val modifiedAt: Instant = now(),
    val deletedAt: Instant? = null,
    val name: String = "",
    val description: String = "",
    val address: String = "",
    val latitude: Double? = null,
    val longitude: Double? = null
) {
    /**
     * Enumerates the [Building] fields that map to a localized display label.
     *
     * @property translationKey The [Translation.key] used to look up the localized label for this field.
     */
    @Serializable
    enum class Property(val translationKey: String) {
        UUID(translationKey = "building_uuid"),
        MODIFIED_AT(translationKey = "building_modified_at"),
        DELETED_AT(translationKey = "building_deleted_at"),
        NAME(translationKey = "building_name"),
        DESCRIPTION(translationKey = "building_description"),
        ADDRESS(translationKey = "building_address"),
        LATITUDE(translationKey = "building_latitude"),
        LONGITUDE(translationKey = "building_longitude")
    }
}
