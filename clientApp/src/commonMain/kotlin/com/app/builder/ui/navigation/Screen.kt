package com.app.builder.ui.navigation

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import androidx.navigation3.runtime.NavKey

/**
 * All possible screens in the application.
 * This is used by the navigation component to define the different destinations.
 * Each object or data class represents a distinct screen and can carry arguments required by that screen.
 */
@Serializable
sealed interface Screen: NavKey {
    /** The splash screen shown while the app is initializing. */
    @Serializable
    @SerialName(value = "screen_splash")
    data object Splash: Screen
    /**
     * The screen shown when either a user want to send feedback or an unrecoverable error occurs.
     *
     * @property error true if the user was redirected to this screen because of an error, false if the user actively navigated to this screen.
     */
    @Serializable
    @SerialName(value = "screen_error")
    data class Error(val error: Boolean): Screen
    /** The home screen. */
    @Serializable
    @SerialName(value = "screen_home")
    data object Home: Screen
    /** The login screen. */
    @Serializable
    @SerialName(value = "screen_login")
    data object Login: Screen
    /** The translations screen. */
    @Serializable
    @SerialName(value = "screen_translations")
    data object Translations: Screen
    /** The push notifications screen. */
    @Serializable
    @SerialName(value = "screen_push")
    data object Push: Screen
    /** The screen listing client-side feature flags. */
    @Serializable
    @SerialName(value = "screen_client_flags")
    data object ClientFlags: Screen
    /** The screen listing client-side configs. */
    @Serializable
    @SerialName(value = "screen_client_configs")
    data object ClientConfigs: Screen
    /** The screen listing server-side feature flags. */
    @Serializable
    @SerialName(value = "screen_server_flags")
    data object ServerFlags: Screen
    /** The screen listing server-side configs. */
    @Serializable
    @SerialName(value = "screen_server_configs")
    data object ServerConfigs: Screen
    /** The screen listing all registries. */
    @Serializable
    @SerialName(value = "screen_registry_list")
    data object RegistryList: Screen
    /**
     * The screen showing the details of a single registry.
     *
     * @property uuid The unique identifier of the registry to display.
     */
    @Serializable
    @SerialName(value = "screen_registry_detail")
    data class RegistryDetail(val uuid: String): Screen
    /** User profile screen. */
    @Serializable
    @SerialName(value = "screen_user_profile")
    data object UserProfile: Screen
    /** The screen listing all users. */
    @Serializable
    @SerialName(value = "screen_user_list")
    data object UserList: Screen
    /**
     * The screen showing the details of a single user.
     *
     * @property uuid The unique identifier of the user to display.
     */
    @Serializable
    @SerialName(value = "screen_user_detail")
    data class UserDetail(val uuid: String): Screen
    /** A screen with device location capture. */
    @Serializable
    @SerialName(value = "screen_device_location")
    data object DeviceLocation: Screen
    /** A screen with nfc scan. */
    @Serializable
    @SerialName(value = "screen_nfc")
    data object NFC: Screen
    /** A screen with a camera. */
    @Serializable
    @SerialName(value = "screen_camera")
    data object Camera: Screen
    /** A screen with audio playback. */
    @Serializable
    @SerialName(value = "screen_audio")
    data object Audio: Screen
    /** The screen listing all tasks. */
    @Serializable
    @SerialName(value = "screen_task_list")
    data object TaskList: Screen
    /**
     * The screen showing the details of a single task.
     *
     * @property uuid The unique identifier of the task to display.
     */
    @Serializable
    @SerialName(value = "screen_task_detail")
    data class TaskDetail(val uuid: String): Screen
    /** The screen listing all files. */
    @Serializable
    @SerialName(value = "screen_file_list")
    data object FileList: Screen
    /**
     * The screen showing the details of a single file.
     *
     * @property uuid The unique identifier of the file to display.
     */
    @Serializable
    @SerialName(value = "screen_file_detail")
    data class FileDetail(val uuid: String): Screen
}
