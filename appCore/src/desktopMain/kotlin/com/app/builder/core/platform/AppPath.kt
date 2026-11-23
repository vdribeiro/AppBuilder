package com.app.builder.core.platform

import java.io.File
import com.app.builder.App
import com.app.builder.core.telemetry.Telemetry

actual val appDataPath: String by lazy {
    val baseDir = when (platform.os) {
        OS.Windows -> getEnv("LOCALAPPDATA") ?: "${getProperty("user.home")}/AppData/Local"
        OS.Mac -> "${getProperty("user.home")}/Library/Application Support"
        OS.Linux -> getEnv("XDG_DATA_HOME").takeIf { !it.isNullOrBlank() } ?: "${getProperty("user.home")}/.local/share"
        else -> "${getProperty("user.home")}/.local/share"
    }
    resolvePath(baseDir = baseDir, folder = "data".takeIf { platform.os == OS.Windows }, label = "data")
        .also { if (it.isNotBlank()) excludeFromBackup(path = it) }
}

actual val appCachePath: String by lazy {
    val baseDir = when (platform.os) {
        OS.Windows -> getEnv("LOCALAPPDATA") ?: "${getProperty("user.home")}/AppData/Local"
        OS.Mac -> "${getProperty("user.home")}/Library/Caches"
        OS.Linux -> getEnv("XDG_CACHE_HOME").takeIf { !it.isNullOrBlank() } ?: "${getProperty("user.home")}/.cache"
        else -> "${getProperty("user.home")}/.cache"
    }
    resolvePath(baseDir = baseDir, folder = "cache".takeIf { platform.os == OS.Windows }, label = "cache")
}

/**
 * Resolves and creates the app-specific directory under [baseDir].
 *
 * @param baseDir The platform's OS-specific base directory (data or cache root).
 * @param folder The folder inside the app-specific directory to resolve, or `null` for the app-specific directory itself.
 * @param label A short name for the directory, used only in error messages (e.g. "data", "cache").
 * @return The absolute path to the app's directory, or an empty string on error.
 */
private fun resolvePath(baseDir: String, folder: String?, label: String): String = runCatching {
    val appDir = App.name
        .lowercase()
        .replace(regex = "\\s+".toRegex(), replacement = "")
    File(baseDir, appDir)
        .let { if (folder != null) File(it, folder) else it }
        .also { if (!it.exists()) it.mkdirs() }
        .takeIf { it.exists() }
        ?.absolutePath
        ?: throw IllegalStateException("App $label directory does not exist")
}.onFailure {
    Telemetry.error(tag = TAG, message = "Unable to get app $label path", throwable = it)
}.getOrDefault(defaultValue = "")

/**
 * Keeps the directory at [path], and everything inside it, out of backups as far as the operating system allows.
 * On macOS the directory is excluded from Time Machine.
 * On Linux it is marked with the files backup tools look for to skip a directory, which only the tools that honor them obey.
 * Does nothing on Windows, which has no backup exclusion an app can set.
 *
 * @param path The absolute path of the directory.
 * @return `true` if the directory is excluded or marked, `false` otherwise.
 */
private fun excludeFromBackup(path: String): Boolean = runCatching {
    when (platform.os) {
        OS.Mac -> runCommand(command = listOf("tmutil", "addexclusion", path)) ?: error(message = "tmutil failed")
        OS.Linux -> {
            File(path, "CACHEDIR.TAG").takeIf { !it.exists() }?.writeText(text = "${"Signature: 8a477f597d28d172789f06886806bc55"}\n")
            File(path, ".deja-dup-ignore").createNewFile()
        }

        else -> return false
    }
    true
}.onFailure {
    Telemetry.error(tag = TAG, message = "Unable to exclude $path from backups", throwable = it)
}.getOrDefault(defaultValue = false)

private const val TAG = "AppPath"
