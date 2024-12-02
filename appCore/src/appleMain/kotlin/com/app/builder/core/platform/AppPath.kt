package com.app.builder.core.platform

import platform.Foundation.NSCachesDirectory
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSSearchPathDirectory
import platform.Foundation.NSSearchPathForDirectoriesInDomains
import platform.Foundation.NSUserDomainMask
import com.app.builder.core.telemetry.Telemetry

actual val appDataPath: String by lazy { resolvePath(directory = NSDocumentDirectory, label = "data") }

actual val appCachePath: String by lazy { resolvePath(directory = NSCachesDirectory, label = "cache") }

/**
 * Resolves the absolute path for the given search-path [directory], creating it if necessary.
 *
 * @param directory The `NSSearchPathDirectory` to resolve.
 * @param label A short label identifying the directory, used in error messages.
 * @return The resolved absolute path, or an empty string on error.
 */
private fun resolvePath(directory: NSSearchPathDirectory, label: String): String = runCatching {
    val fileManager = NSFileManager.defaultManager
    val paths = NSSearchPathForDirectoriesInDomains(
        directory = directory,
        domainMask = NSUserDomainMask,
        expandTilde = true
    )
    paths.firstOrNull()?.let { it as String }
        ?.also {
            if (!fileManager.fileExistsAtPath(path = it)) {
                val success = fileManager.createDirectoryAtPath(
                    path = it,
                    withIntermediateDirectories = true,
                    attributes = null,
                    error = null
                )
                if (!success) throw IllegalStateException("Unable to create directory")
            }
        }
        ?.takeIf { fileManager.fileExistsAtPath(path = it) }
        ?: throw IllegalStateException("App $label directory does not exist")
}.onFailure {
    Telemetry.error(tag = TAG, message = "Unable to get app $label path", throwable = it)
}.getOrDefault(defaultValue = "")

private const val TAG = "AppPath"
