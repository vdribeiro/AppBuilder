package com.app.builder.core.platform

import java.io.File
import com.app.builder.App
import com.app.builder.core.telemetry.Telemetry

actual val appDataPath: String by lazy {
    val baseDir = when (platform.os) {
        OS.Windows -> getEnv("APPDATA") ?: "${getProperty("user.home")}/AppData/Roaming"
        OS.Mac -> "${getProperty("user.home")}/Library/Application Support"
        OS.Linux -> getEnv("XDG_DATA_HOME").takeIf { !it.isNullOrBlank() } ?: "${getProperty("user.home")}/.local/share"
        else -> "${getProperty("user.home")}/.local/share"
    }
    resolvePath(baseDir = baseDir, label = "data")
}

actual val appCachePath: String by lazy {
    val baseDir = when (platform.os) {
        OS.Windows -> getEnv("LOCALAPPDATA") ?: "${getProperty("user.home")}/AppData/Local"
        OS.Mac -> "${getProperty("user.home")}/Library/Caches"
        OS.Linux -> getEnv("XDG_CACHE_HOME").takeIf { !it.isNullOrBlank() } ?: "${getProperty("user.home")}/.cache"
        else -> "${getProperty("user.home")}/.cache"
    }
    resolvePath(baseDir = baseDir, label = "cache")
}

/**
 * Resolves and creates the app-specific directory under [baseDir].
 *
 * @param baseDir The platform's OS-specific base directory (data or cache root).
 * @param label A short name for the directory, used only in error messages (e.g. "data", "cache").
 * @return The absolute path to the app's directory, or an empty string on error.
 */
private fun resolvePath(baseDir: String, label: String): String = runCatching {
    val appDir = App.name
        .lowercase()
        .replace(regex = "\\s+".toRegex(), replacement = "")
    File(baseDir, appDir)
        .also { if (!it.exists()) it.mkdirs() }
        .takeIf { it.exists() }
        ?.absolutePath
        ?: throw IllegalStateException("App $label directory does not exist")
}.onFailure {
    Telemetry.error(tag = TAG, message = "Unable to get app $label path", throwable = it)
}.getOrDefault(defaultValue = "")

private const val TAG = "AppPath"
