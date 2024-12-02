package com.app.builder.ui.screen.userprofile

import com.app.builder.AppInfo
import com.app.builder.core.locale.getLanguage
import com.app.builder.ui.core.image.Image

/** The set of user-triggered intents available within the user profile. */
sealed interface UserProfileScreenAction {
    /** Requests to edit the current user's profile. */
    data object EditProfile: UserProfileScreenAction
    /** Requests to change the current user's password. */
    object ChangePassword: UserProfileScreenAction
    /** Requests to open notification settings. */
    object Notifications: UserProfileScreenAction
    /** Logs out the current user. */
    object Logout: UserProfileScreenAction
    /** Resets the app's local data. */
    object Reset: UserProfileScreenAction
}

/**
 * State for the user profile screen, holding the current user's details alongside locale and app info.
 *
 * @property guest true if it is a guest account, false otherwise.
 * @property name Name of the current user.
 * @property image Image of the current user's avatar.
 * @property language Current language/locale code.
 * @property appVersion Current app version.
 */
data class UserProfileScreenState(
    val guest: Boolean = true,
    val name: String? = null,
    val image: Image? = null,
    val language: String = getLanguage(),
    val appVersion: String = AppInfo.VERSION,
)
