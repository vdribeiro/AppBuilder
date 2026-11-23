package com.app.builder.core.platform

import kotlinx.cinterop.ObjCObjectVar
import kotlinx.cinterop.alloc
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.value
import platform.Foundation.NSCachesDirectory
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSError
import platform.Foundation.NSFileManager
import platform.Foundation.NSNumber
import platform.Foundation.NSSearchPathDirectory
import platform.Foundation.NSSearchPathForDirectoriesInDomains
import platform.Foundation.NSURL
import platform.Foundation.NSURLIsExcludedFromBackupKey
import platform.Foundation.NSUserDomainMask
import com.app.builder.core.telemetry.Telemetry

actual val appDataPath: String by lazy {
    resolvePath(directory = NSDocumentDirectory, label = "data").also { if (it.isNotBlank()) excludeFromBackup(path = it) }
}

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

/**
 * Excludes the directory at [path], and everything inside it, from the backups of the device.
 *
 * @param path The absolute path of the directory.
 * @return `true` if the directory is excluded, `false` otherwise.
 */
fun excludeFromBackup(path: String): Boolean = runCatching {
    memScoped {
        val nsError = alloc<ObjCObjectVar<NSError?>>()
        val excluded = NSURL.fileURLWithPath(path = path, isDirectory = true).setResourceValue(
            value = NSNumber(bool = true),
            forKey = NSURLIsExcludedFromBackupKey,
            error = nsError.ptr
        )
        if (!excluded) error(message = nsError.value?.localizedDescription ?: "Unknown error")
        true
    }
}.onFailure {
    Telemetry.error(tag = TAG, message = "Unable to exclude $path from backups", throwable = it)
}.getOrDefault(defaultValue = false)

private const val TAG = "AppPath"
