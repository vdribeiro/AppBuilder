package com.app.builder.ui.navigation.userprofile

import kotlin.uuid.Uuid
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

/** Actions supported by the Change Password Screen. */
sealed interface ChangePasswordScreenAction {
    /**
     * Updates the current password input field.
     *
     * @param password The new current password value.
     */
    data class UpdateCurrentPassword(val password: String): ChangePasswordScreenAction
    /**
     * Updates the new password input field.
     *
     * @param password The new password value.
     */
    data class UpdateNewPassword(val password: String): ChangePasswordScreenAction
    /**
     * Updates the new password confirmation input field.
     *
     * @param password The new confirmation value.
     */
    data class UpdateConfirmPassword(val password: String): ChangePasswordScreenAction
    /** Triggers the password change submission process. */
    data object Submit: ChangePasswordScreenAction
    /** Dismisses the message currently shown to the user. */
    data object DismissMessage: ChangePasswordScreenAction
}

/**
 * State of the Change Password Screen.
 *
 * @property userUuid Unique identifier of the current user, or `null` when no user is authenticated.
 * @property loading Whether a password change request is currently in flight.
 * @property currentPassword Current password input field value.
 * @property newPassword New password input field value.
 * @property confirmPassword New password confirmation input field value.
 * @property message Translation key of the message shown to the user, or null when there is nothing to report.
 * @property messageArgs Arguments the message is formatted with.
 */
data class ChangePasswordScreenState(
    val userUuid: Uuid? = null,
    val loading: Boolean = false,
    val currentPassword: String = "",
    val newPassword: String = "",
    val confirmPassword: String = "",
    val message: String? = null,
    val messageArgs: ImmutableList<String> = persistentListOf(),
)
