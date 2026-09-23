package com.app.builder.data.storage

import kotlinx.browser.localStorage
import kotlinx.coroutines.withContext
import com.app.builder.core.flow.Dispatcher
import com.app.builder.core.platform.appCachePath
import com.app.builder.core.platform.appDataPath
import com.app.builder.core.telemetry.Telemetry

actual suspend fun saveFile(path: String, content: String): Boolean = withContext(context = Dispatcher.IO) {
    runCatching {
        localStorage.setItem(key = "${appDataPath}_$path", value = content)
        true
    }.onFailure {
        Telemetry.error(tag = TAG, message = "Unable to save file $path", throwable = it)
    }.getOrDefault(defaultValue = false)
}

actual suspend fun loadFile(path: String): String? = withContext(context = Dispatcher.IO) {
    runCatching {
        localStorage.getItem(key = "${appDataPath}_$path")
    }.onFailure {
        Telemetry.error(tag = TAG, message = "Unable to load file $path", throwable = it)
    }.getOrNull()
}

actual suspend fun deleteFile(path: String): Boolean = withContext(context = Dispatcher.IO) {
    runCatching {
        localStorage.removeItem(key = "${appDataPath}_$path")
        true
    }.onFailure {
        Telemetry.error(tag = TAG, message = "Unable to delete file $path", throwable = it)
    }.getOrDefault(defaultValue = false)
}

actual suspend fun clearCache(): Boolean = withContext(context = Dispatcher.IO) {
    runCatching {
        (0 until localStorage.length)
            .mapNotNull { localStorage.key(index = it) }
            .filter { it.startsWith(prefix = "${appCachePath}_") }
            .forEach { localStorage.removeItem(key = it) }
        true
    }.onFailure {
        Telemetry.error(tag = TAG, message = "Unable to clear cache", throwable = it)
    }.getOrDefault(defaultValue = false)
}

actual suspend fun listFiles(path: String): List<DeviceFile> = withContext(context = Dispatcher.IO) {
    runCatching {
        val prefix = "${path}_"
        (0 until localStorage.length)
            .mapNotNull { localStorage.key(index = it) }
            .filter { it.startsWith(prefix = prefix) }
            .map { key ->
                DeviceFile(
                    path = key,
                    name = key.removePrefix(prefix = prefix),
                    size = localStorage.getItem(key = key)?.length?.toLong() ?: 0L,
                    modifiedAt = null
                )
            }
    }.onFailure {
        Telemetry.error(tag = TAG, message = "Unable to list files in $path", throwable = it)
    }.getOrDefault(defaultValue = emptyList())
}

// The browser has no file system.
actual suspend fun openFile(path: String): Boolean = false

private const val TAG = "File"
