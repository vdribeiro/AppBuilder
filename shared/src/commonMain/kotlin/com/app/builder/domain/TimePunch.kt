package com.app.builder.domain

import kotlin.time.Instant
import kotlin.uuid.Uuid
import kotlinx.serialization.Serializable
import com.app.builder.core.locale.now
import com.app.builder.core.security.uuid

/**
 * Time punch entity, a point in time at which a user punched the clock.
 *
 * @property uuid The unique, immutable identifier for this time punch.
 * @property modifiedAt The timestamp indicating when this time punch was last updated.
 * @property deletedAt The timestamp indicating when this time punch was last deleted.
 * @property userUuid The unique identifier of the user that punched the clock.
 * @property time The UTC at which the user punched the clock.
 * @property source The time punch source.
 */
@Serializable
data class TimePunch(
    val uuid: Uuid = uuid(),
    val modifiedAt: Instant = now(),
    val deletedAt: Instant? = null,
    val userUuid: Uuid,
    val time: Instant = now(),
    val source: Source = Source.MANUAL
) {
    /**
     * Time punch source.
     *
     * @property translationKey The [Translation.key] used to look up the localized label for this field.
     */
    @Serializable
    enum class Source(val translationKey: String) {
        MANUAL(translationKey = "time_punch_source_manual"),
        NFC(translationKey = "time_punch_source_nfc")
    }

    /**
     * Enumerates the [TimePunch] fields that map to a localized display label.
     *
     * @property translationKey The [Translation.key] used to look up the localized label for this field.
     */
    @Serializable
    enum class Property(val translationKey: String) {
        UUID(translationKey = "time_punch_uuid"),
        MODIFIED_AT(translationKey = "time_punch_modified_at"),
        DELETED_AT(translationKey = "time_punch_deleted_at"),
        USER_UUID(translationKey = "time_punch_user_uuid"),
        USER_NAME(translationKey = "time_punch_user_name"),
        TIME(translationKey = "time_punch_time"),
        SOURCE(translationKey = "time_punch_source")
    }
}
