package com.app.builder.core.platform

import android.os.Build

actual val platform: Platform by lazy {
    val osVersion = runCatching { Build.VERSION.SDK_INT.toString() }.getOrDefault(defaultValue = "unknown")
    val brand = runCatching { Build.BRAND }.getOrNull()?.takeIf { it.isNotBlank() } ?: "unknown"
    val model = runCatching { Build.MODEL }.getOrNull()?.takeIf { it.isNotBlank() } ?: "unknown"

    Platform(
        os = OS.Android,
        osVersion = osVersion,
        brand = brand,
        model = model,
    )
}
