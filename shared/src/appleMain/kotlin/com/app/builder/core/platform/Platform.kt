package com.app.builder.core.platform

import platform.Foundation.NSProcessInfo
import platform.UIKit.UIDevice

actual val platform: Platform by lazy {
    val osVersion = runCatching { NSProcessInfo.processInfo.operatingSystemVersionString }.getOrDefault(defaultValue = "unknown")
    val model = runCatching { UIDevice.currentDevice.model }.getOrNull()?.takeIf { it.isNotBlank() } ?: "unknown"

    Platform(
        os = OS.Ios,
        osVersion = osVersion,
        brand = "Apple",
        model = model
    )
}

