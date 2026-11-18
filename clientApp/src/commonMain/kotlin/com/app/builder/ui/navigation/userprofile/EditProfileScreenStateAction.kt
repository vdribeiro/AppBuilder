package com.app.builder.ui.navigation.userprofile

import com.app.builder.domain.User

/** Actions supported by the Edit Profile Screen. */
sealed interface EditProfileScreenAction {
    /**
     * Updates the name input field.
     *
     * @param name The new name value.
     */
    data class UpdateName(val name: String): EditProfileScreenAction
    /**
     * Updates the avatar URL input field.
     *
     * @param avatar The new avatar URL value.
     */
    data class UpdateAvatar(val avatar: String): EditProfileScreenAction
    /** Triggers the profile save process. */
    data object Submit: EditProfileScreenAction
}

/**
 * State of the Edit Profile Screen.
 *
 * @property user The stored current user, or `null` when no user is authenticated.
 * @property name Name input field value.
 * @property avatar Avatar URL input field value.
 */
data class EditProfileScreenState(
    val user: User? = null,
    val name: String = "",
    val avatar: String = "",
)
