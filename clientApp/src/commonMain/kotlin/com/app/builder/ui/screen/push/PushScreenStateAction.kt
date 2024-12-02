package com.app.builder.ui.screen.push

import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import com.app.builder.ui.component.button.PushPayloadType
import com.app.builder.ui.component.button.UserItem

/**
 * State for the push notification screen, holding the payload being composed and its send status.
 *
 * @property type Type of push payload to send.
 * @property broadcast Whether the push is sent to all users or only to [selectedUsers].
 * @property users All users available to select as recipients.
 * @property selectedUsers Users currently selected as recipients, when not broadcasting.
 * @property notificationTitle Notification title.
 * @property notificationDescription Notification description.
 * @property sending Whether a send request is currently in flight.
 * @property sent Number of pushes successfully sent, or `null` if none have been sent yet.
 */
data class PushScreenState(
    val type: PushPayloadType = PushPayloadType.NOTIFICATION,
    val broadcast: Boolean = true,
    val users: ImmutableList<UserItem> = persistentListOf(),
    val selectedUsers: ImmutableList<UserItem> = persistentListOf(),
    val notificationTitle: String = "",
    val notificationDescription: String = "",
    val sending: Boolean = false,
    val sent: Int? = null,
)

/** Actions dispatched by the push notification screen UI. */
sealed interface PushScreenAction {
    /**
     * Selects the type of push payload to send.
     *
     * @param type Selected push payload type.
     */
    data class SelectType(val type: PushPayloadType): PushScreenAction
    /**
     * Toggles whether the push is broadcast to all users or sent to selected users only.
     *
     * @param value New broadcast flag value.
     */
    data class ToggleBroadcast(val value: Boolean): PushScreenAction
    /**
     * Toggles whether the given [user] is included among the selected recipients.
     *
     * @param user User whose selection should be toggled.
     */
    data class ToggleUser(val user: UserItem): PushScreenAction
    /**
     * Updates the notification title field.
     *
     * @param value New title value.
     */
    data class UpdateTitle(val value: String): PushScreenAction
    /**
     * Updates the notification description field.
     *
     * @param value New description value.
     */
    data class UpdateDescription(val value: String): PushScreenAction
    /** Triggers sending the composed push payload(s). */
    data object Send: PushScreenAction
}
