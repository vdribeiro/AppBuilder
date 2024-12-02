package com.app.builder.core.platform

actual val platform: Platform by lazy {
    val osName = getProperty("os.name").orEmpty()
    val os = when {
        osName.isBlank() -> OS.Unknown
        osName.contains(other = "win", ignoreCase = true) -> OS.Windows
        osName.contains(other = "mac", ignoreCase = true) -> OS.Mac
        osName.contains(other = "nix", ignoreCase = true) ||
                osName.contains(other = "nux", ignoreCase = true) ||
                osName.contains(other = "aix", ignoreCase = true) -> OS.Linux

        else -> OS.Unknown
    }
    val osVersion = getProperty("os.version")?.takeIf { it.isNotBlank() } ?: "unknown"
    val brand = getProperty("java.vendor")?.takeIf { it.isNotBlank() } ?: "unknown"
    val model = getProperty("os.arch")?.takeIf { it.isNotBlank() } ?: "unknown"

    Platform(
        os = os,
        osVersion = osVersion,
        brand = brand,
        model = model
    )
}
