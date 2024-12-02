package com.app.builder.domain

import kotlin.uuid.Uuid
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import com.app.builder.core.security.uuid

/** A type-safe representation of all types of push data. */
@Serializable
sealed interface PushPayload {

    companion object {
        /** The data key carrying the encoded [PushPayload] JSON. */
        const val DATA_KEY = "payload"
    }

    /** The unique identifier for this notification. */
    val uuid: Uuid
    /** The unique identifier for the user associated with this notification. */
    val userUuid: Uuid?
    /** The title of the notification */
    val title: String
    /** The description of the notification */
    val description: String

    /**
     * Represents a general notification.
     *
     * @property uuid The unique identifier for this notification.
     * @property userUuid The unique identifier for the user associated with this notification.
     * @property title The title of the notification.
     * @property description The description of the notification.
     */
    @Serializable
    @SerialName(value = "notification")
    data class Notification(
        override val uuid: Uuid = uuid(),
        override val userUuid: Uuid? = null,
        override val title: String,
        override val description: String,
    ): PushPayload

    /**
     * Represents an entity notification.
     *
     * @property uuid The unique identifier for this notification.
     * @property userUuid The unique identifier for the user associated with this notification.
     * @property title The title of the notification.
     * @property description The description of the notification.
     * @property entityType The type of the associated entity.
     * @property entityUuid The unique identifier for the associated entity, if any.
     */
    @Serializable
    @SerialName(value = "entity_notification")
    data class EntityNotification(
        override val uuid: Uuid = uuid(),
        override val userUuid: Uuid,
        override val title: String,
        override val description: String,
        val entityType: EntityType,
        val entityUuid: Uuid? = null,
    ): PushPayload

    /**
     * Indicative of a flags update.
     *
     * @property uuid The unique identifier for this notification.
     * @property userUuid The unique identifier for the user associated with this notification.
     * @property title The title of the notification.
     * @property description The description of the notification.
     */
    @Serializable
    @SerialName(value = "flags")
    data class Flags(
        override val uuid: Uuid = uuid(),
        override val userUuid: Uuid? = null,
        override val title: String = "flags",
        override val description: String = "",
    ): PushPayload

    /**
     * Indicative of a configs update.
     *
     * @property uuid The unique identifier for this notification.
     * @property userUuid The unique identifier for the user associated with this notification.
     * @property title The title of the notification.
     * @property description The description of the notification.
     */
    @Serializable
    @SerialName(value = "configs")
    data class Configs(
        override val uuid: Uuid = uuid(),
        override val userUuid: Uuid? = null,
        override val title: String = "configs",
        override val description: String = "",
    ): PushPayload

    /**
     * Request a client reset.
     *
     * @property uuid The unique identifier for this notification.
     * @property userUuid The unique identifier for the user associated with this notification.
     * @property title The title of the notification.
     * @property description The description of the notification.
     */
    @Serializable
    @SerialName(value = "reset")
    data class Reset(
        override val uuid: Uuid = uuid(),
        override val userUuid: Uuid? = null,
        override val title: String = "reset",
        override val description: String = "",
    ): PushPayload
}
