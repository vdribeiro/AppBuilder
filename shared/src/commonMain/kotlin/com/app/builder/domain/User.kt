package com.app.builder.domain

import kotlin.time.Instant
import kotlin.uuid.Uuid
import kotlinx.serialization.Serializable

/**
 * A person, who may or may not hold an [Account] to log in with.
 *
 * @property uuid The unique identifier for the user.
 * @property modifiedAt The timestamp indicating when this user was last updated.
 * @property deletedAt The timestamp indicating when this user was last deleted.
 * @property name The display name of the user.
 * @property avatar The URL to the user's profile image, or null if not set.
 */
@Serializable
data class User(
    val uuid: Uuid,
    val modifiedAt: Instant,
    val deletedAt: Instant?,
    val name: String,
    val avatar: String?,
) {
    /**
     * Enumerates the [User] fields that map to a localized display label.
     *
     * @property translationKey The [Translation.key] used to look up the localized label for this field.
     */
    @Serializable
    enum class Property(val translationKey: String) {
        UUID(translationKey = "user_uuid"),
        MODIFIED_AT(translationKey = "user_modified_at"),
        DELETED_AT(translationKey = "user_deleted_at"),
        NAME(translationKey = "user_name")
    }
}