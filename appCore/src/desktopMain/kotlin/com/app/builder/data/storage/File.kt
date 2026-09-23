package com.app.builder.data.storage

import java.awt.Desktop
import java.io.File
import kotlinx.coroutines.withContext
import com.app.builder.core.flow.Dispatcher
import com.app.builder.core.platform.appCachePath
import com.app.builder.core.platform.appDataPath
import com.app.builder.core.telemetry.Telemetry

actual suspend fun saveFile(path: String, content: String): Boolean = withContext(context = Dispatcher.IO) {
    runCatching {
        val file = File(appDataPath, path)
        file.writeText(text = content)
        true
    }.onFailure {
        Telemetry.error(tag = TAG, message = "Unable to save file $path", throwable = it)
    }.getOrDefault(defaultValue = false)
}

actual suspend fun loadFile(path: String): String? = withContext(context = Dispatcher.IO) {
    runCatching {
        val file = File(appDataPath, path)
        if (file.exists() && file.isFile) file.readText() else null
    }.onFailure {
        Telemetry.error(tag = TAG, message = "Unable to load file $path", throwable = it)
    }.getOrNull()
}

actual suspend fun deleteFile(path: String): Boolean = withContext(context = Dispatcher.IO) {
    runCatching {
        val file = File(appDataPath, path)
        if (file.exists()) file.delete() else true
    }.onFailure {
        Telemetry.error(tag = TAG, message = "Unable to delete file $path", throwable = it)
    }.getOrDefault(defaultValue = false)
}

actual suspend fun clearCache(): Boolean = withContext(context = Dispatcher.IO) {
    runCatching {
        File(appCachePath).listFiles()?.forEach { it.deleteRecursively() }
        true
    }.onFailure {
        Telemetry.error(tag = TAG, message = "Unable to clear cache", throwable = it)
    }.getOrDefault(defaultValue = false)
}

actual suspend fun listFiles(path: String): List<DeviceFile> = withContext(context = Dispatcher.IO) {
    runCatching {
        File(path).walkTopDown()
            .filter { it.isFile }
            .map { DeviceFile(path = it.absolutePath, name = it.name, size = it.length(), modifiedAt = it.lastModified()) }
            .toList()
    }.onFailure {
        Telemetry.error(tag = TAG, message = "Unable to list files in $path", throwable = it)
    }.getOrDefault(defaultValue = emptyList())
}

actual suspend fun openFile(path: String): Boolean = withContext(context = Dispatcher.IO) {
    runCatching {
        val file = File(path)
        if (!file.exists()) error(message = "File does not exist")
        if (!Desktop.isDesktopSupported() || !Desktop.getDesktop().isSupported(Desktop.Action.OPEN)) error(message = "No desktop handler available")
        Desktop.getDesktop().open(file)
        true
    }.onFailure {
        Telemetry.error(tag = TAG, message = "Unable to open file $path", throwable = it)
    }.getOrDefault(defaultValue = false)
}

private const val TAG = "File"
