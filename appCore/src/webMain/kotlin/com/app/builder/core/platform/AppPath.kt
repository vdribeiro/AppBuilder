package com.app.builder.core.platform

import com.app.builder.App

/** Lowercased, whitespace-stripped app name used as a prefix for browser storage keys. */
private val appName: String by lazy {
    App.name.lowercase().replace(regex = "\\s+".toRegex(), replacement = "")
}

actual val appDataPath: String by lazy { "${appName}_storage" }

actual val appCachePath: String by lazy { "${appName}_cache" }
