package com.app.builder.ui.screen.devicefiles

import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import com.app.builder.ui.component.list.DeviceFileItem

/** Actions that can be dispatched to a device files store. */
sealed interface DeviceFilesScreenAction {
    /** Reloads the files held in the application's directories. */
    data object Refresh: DeviceFilesScreenAction
    /**
     * Hands a file over to the platform's default handler for its type.
     *
     * @param path The absolute path of the file to open.
     */
    data class OpenFile(val path: String): DeviceFilesScreenAction
    /** Dismisses the message currently shown to the user. */
    data object DismissMessage: DeviceFilesScreenAction
}

/**
 * State of the device files screen, holding the files found in the application's directories.
 *
 * @property files Files currently displayed, newest first.
 * @property message Message shown to the user, or null when there is nothing to report.
 */
data class DeviceFilesScreenState(
    val files: ImmutableList<DeviceFileItem> = persistentListOf(),
    val message: String? = null
)
