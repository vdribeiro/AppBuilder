package com.app.builder.core.platform

import com.app.builder.applicationContext
import com.app.builder.core.telemetry.Telemetry

actual val appDataPath: String by lazy {
    runCatching {
        applicationContext.filesDir
            .also { if (!it.exists()) it.mkdirs() }
            .takeIf { it.exists() }
            ?.absolutePath
            ?: throw IllegalStateException("App data directory does not exist")
    }.onFailure {
        Telemetry.error(tag = TAG, message = "Unable to get app data path", throwable = it)
    }.getOrDefault(defaultValue = "")
}

actual val appCachePath: String by lazy {
    runCatching {
        applicationContext.cacheDir
            .also { if (!it.exists()) it.mkdirs() }
            .takeIf { it.exists() }
            ?.absolutePath
            ?: throw IllegalStateException("App cache directory does not exist")
    }.onFailure {
        Telemetry.error(tag = TAG, message = "Unable to get app cache path", throwable = it)
    }.getOrDefault(defaultValue = "")
}

private const val TAG = "AppPath"
