package com.app.builder.domain

import kotlin.time.Instant
import kotlin.uuid.Uuid
import kotlinx.serialization.Serializable
import com.app.builder.core.locale.now
import com.app.builder.core.security.uuid

/**
 * Equipment entity.
 *
 * @property uuid The unique, immutable identifier for this equipment.
 * @property modifiedAt The timestamp indicating when this equipment was last updated.
 * @property deletedAt The timestamp indicating when this equipment was last deleted.
 * @property name The name of the equipment.
 * @property description The descriptive content detailing the equipment.
 * @property status The operating status of the equipment.
 */
@Serializable
data class Equipment(
    val uuid: Uuid = uuid(),
    val modifiedAt: Instant = now(),
    val deletedAt: Instant? = null,
    val name: String = "",
    val description: String = "",
    val status: Status = Status.WORKING
) {
    /**
     * Equipment operating status.
     *
     * @property translationKey The [Translation.key] used to look up the localized label for this field.
     */
    @Serializable
    enum class Status(val translationKey: String) {
        WORKING(translationKey = "equipment_status_working"),
        MALFUNCTIONING(translationKey = "equipment_status_malfunctioning")
    }

    /**
     * Enumerates the [Equipment] fields that map to a localized display label.
     *
     * @property translationKey The [Translation.key] used to look up the localized label for this field.
     */
    @Serializable
    enum class Property(val translationKey: String) {
        UUID(translationKey = "equipment_uuid"),
        MODIFIED_AT(translationKey = "equipment_modified_at"),
        DELETED_AT(translationKey = "equipment_deleted_at"),
        NAME(translationKey = "equipment_name"),
        DESCRIPTION(translationKey = "equipment_description"),
        STATUS(translationKey = "equipment_status")
    }
}
