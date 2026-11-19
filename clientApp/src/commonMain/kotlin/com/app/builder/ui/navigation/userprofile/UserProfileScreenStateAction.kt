package com.app.builder.ui.navigation.userprofile

import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import com.app.builder.AppInfo
import com.app.builder.core.locale.getLanguage
import com.app.builder.ui.core.image.Image

/** The set of user-triggered intents available within the user profile. */
sealed interface UserProfileScreenAction {
    /** Requests to open notification settings. */
    object Notifications: UserProfileScreenAction
    /** Logs out the current user. */
    object Logout: UserProfileScreenAction
    /** Resets the app's local data. */
    object Reset: UserProfileScreenAction

    /**
     * Selects the language the app is shown in.
     *
     * @param languageIso ISO code of the language, or `null` to follow the device language.
     */
    data class SelectLanguage(val languageIso: String?): UserProfileScreenAction
}

/**
 * State for the user profile screen, holding the current user's details alongside locale and app info.
 *
 * @property guest true if it is a guest account, false otherwise.
 * @property name Name of the current user.
 * @property image Image of the current user's avatar.
 * @property language ISO code of the language the app is shown in.
 * @property languages ISO codes of the languages holding translations.
 * @property selectedLanguage ISO code of the language selected for the app, or `null` while the app follows the device language.
 * @property appVersion Current app version.
 */
data class UserProfileScreenState(
    val guest: Boolean = true,
    val name: String? = null,
    val image: Image? = null,
    val language: String = getLanguage(),
    val languages: ImmutableList<String> = persistentListOf(),
    val selectedLanguage: String? = null,
    val appVersion: String = AppInfo.VERSION,
)
