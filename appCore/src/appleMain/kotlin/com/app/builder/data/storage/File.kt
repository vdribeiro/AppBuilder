package com.app.builder.data.storage

import kotlinx.coroutines.withContext
import platform.Foundation.NSFileManager
import platform.Foundation.NSString
import platform.Foundation.NSUTF8StringEncoding
import platform.Foundation.create
import platform.Foundation.stringByAppendingPathComponent
import platform.Foundation.stringWithContentsOfFile
import platform.Foundation.writeToFile
import com.app.builder.core.flow.Dispatcher
import com.app.builder.core.platform.appCachePath
import com.app.builder.core.platform.appDataPath
import com.app.builder.core.telemetry.Telemetry

actual suspend fun saveFile(path: String, content: String): Boolean = withContext(context = Dispatcher.IO) {
    runCatching {
        val file = NSString.create(string = appDataPath).stringByAppendingPathComponent(str = path)
        NSString.create(string = content).writeToFile(
            path = file,
            atomically = true,
            encoding = NSUTF8StringEncoding,
            error = null
        )
        true
    }.onFailure {
        Telemetry.error(tag = TAG, message = "Unable to save file $path", throwable = it)
    }.getOrDefault(defaultValue = false)
}

actual suspend fun loadFile(path: String): String? = withContext(context = Dispatcher.IO) {
    runCatching {
        val fullPath = NSString.create(string = appDataPath).stringByAppendingPathComponent(str = path)
        val fileManager = NSFileManager.defaultManager
        if (fileManager.fileExistsAtPath(path = fullPath)) {
            NSString.stringWithContentsOfFile(
                path = fullPath,
                encoding = NSUTF8StringEncoding,
                error = null
            )
        } else null
    }.onFailure {
        Telemetry.error(tag = TAG, message = "Unable to load file $path", throwable = it)
    }.getOrNull()
}

actual suspend fun deleteFile(path: String): Boolean = withContext(context = Dispatcher.IO) {
    runCatching {
        val fullPath = NSString.create(string = appDataPath).stringByAppendingPathComponent(str = path)
        val fileManager = NSFileManager.defaultManager
        if (fileManager.fileExistsAtPath(path = fullPath)) {
            fileManager.removeItemAtPath(
                path = fullPath,
                error = null
            )
        } else true
    }.onFailure {
        Telemetry.error(tag = TAG, message = "Unable to delete file $path", throwable = it)
    }.getOrDefault(defaultValue = false)
}

actual suspend fun clearCache(): Boolean = withContext(context = Dispatcher.IO) {
    runCatching {
        val fileManager = NSFileManager.defaultManager
        fileManager.contentsOfDirectoryAtPath(path = appCachePath, error = null)
            ?.filterIsInstance<String>()
            ?.forEach {
                val itemPath = NSString.create(string = appCachePath).stringByAppendingPathComponent(str = it)
                fileManager.removeItemAtPath(path = itemPath, error = null)
            }
        true
    }.onFailure {
        Telemetry.error(tag = TAG, message = "Unable to clear cache", throwable = it)
    }.getOrDefault(defaultValue = false)
}

private const val TAG = "File"
