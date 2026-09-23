package com.app.builder.ui.screen.devicefiles

import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.Job
import com.app.builder.core.flow.Dispatcher
import com.app.builder.core.locale.getLocalDateTime
import com.app.builder.core.locale.toInstant
import com.app.builder.core.platform.appCachePath
import com.app.builder.core.platform.appDataPath
import com.app.builder.core.telemetry.Telemetry
import com.app.builder.data.storage.DeviceFile
import com.app.builder.data.storage.listFiles
import com.app.builder.data.storage.openFile
import com.app.builder.ui.component.list.DeviceFileItem
import com.app.builder.ui.store.Store

/** Store backing the device files screen, listing the application's own files and handing them over to the platform when opened. */
class DeviceFilesScreenStore(
    state: DeviceFilesScreenState
): Store<DeviceFilesScreenState, DeviceFilesScreenAction>(initialState = state) {
    init {
        setup()
    }

    override fun reducer(state: DeviceFilesScreenState, action: DeviceFilesScreenAction) {
        super.reducer(state = state, action = action)
        when (action) {
            is DeviceFilesScreenAction.Refresh -> setup()
            is DeviceFilesScreenAction.OpenFile -> openDeviceFile(action = action)
            is DeviceFilesScreenAction.DismissMessage -> updateState { it.copy(message = null) }
        }
    }

    /**
     * Lists the files held in the application's cache and data directories, newest first.
     *
     * @return The [Job] representing this execution.
     */
    private fun setup(): Job = launch(id = "setup", context = Dispatcher.IO) {
        Telemetry.info(tag = TAG, message = "Setup")

        val files = (listFiles(path = appCachePath) + listFiles(path = appDataPath))
            .sortedByDescending { it.modifiedAt ?: 0L }
            .map { it.toDeviceFileItem() }
            .toPersistentList()

        updateState { it.copy(files = files) }
    }

    /**
     * Hands the selected file over to the platform, reporting back when no handler took it.
     *
     * @param action The action carrying the path of the file to open.
     * @return The [Job] representing this execution.
     */
    private fun openDeviceFile(action: DeviceFilesScreenAction.OpenFile): Job = launch(id = "openFile") {
        val opened = openFile(path = action.path)
        updateState { it.copy(message = if (opened) null else "unable_to_open_file") }
    }

    /**
     * Converts a [DeviceFile] into the item rendered by the list.
     *
     * @return The [DeviceFileItem] describing this file.
     */
    private fun DeviceFile.toDeviceFileItem(): DeviceFileItem = DeviceFileItem(
        path = path,
        name = name,
        description = listOfNotNull(
            size.toReadableSize(),
            modifiedAt?.toInstant()?.let { getLocalDateTime(utc = it.toString()) }
        ).joinToString(separator = " - ")
    )

    /**
     * Formats a size in bytes into the largest unit that keeps it readable.
     *
     * @return The formatted size, such as `1.2 MB`.
     */
    private fun Long.toReadableSize(): String {
        var size = this.toDouble()
        var unit = 0
        while (size >= 1024 && unit < UNITS.lastIndex) {
            size /= 1024
            unit++
        }
        val rounded = (size * 10).toLong() / 10.0
        return "$rounded ${UNITS[unit]}"
    }

    companion object {
        private const val TAG = "DeviceFilesScreenStore"

        /** Size units, from bytes upwards, each one 1024 times the previous. */
        private val UNITS = listOf("B", "KB", "MB", "GB")
    }
}
