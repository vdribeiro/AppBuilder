package com.app.builder.core.platform

actual val platform: Platform by lazy {
    val userAgent = runCatching { getUserAgent() }.getOrNull()?.takeIf { it.isNotBlank() } ?: "unknown"
    val vendor = runCatching { getVendor() }.getOrNull().orEmpty()
    val brand = when {
        userAgent.contains(other = "Edg", ignoreCase = true) -> "Microsoft Edge"
        userAgent.contains(other = "Chrome", ignoreCase = true) && vendor.contains(other = "Google") -> "Google Chrome"
        userAgent.contains(other = "Firefox", ignoreCase = true) -> "Mozilla Firefox"
        userAgent.contains(other = "Safari", ignoreCase = true) && !userAgent.contains(other = "Chrome") -> "Apple Safari"
        userAgent.contains(other = "Opera", ignoreCase = true) || userAgent.contains(other = "OPR") -> "Opera"
        else -> vendor.takeIf { it.isNotBlank() } ?: "unknown"
    }

    Platform(
        os = OS.Web,
        osVersion = userAgent,
        brand = brand,
        model = "Browser",
    )
}

/**
 * Retrieves the browser's user agent string.
 *
 * @return The value of `window.navigator.userAgent`.
 */
@JsFun(code = "() => window.navigator.userAgent")
private external fun getUserAgent(): String

/**
 * Retrieves the browser vendor name.
 *
 * @return The value of `window.navigator.vendor`.
 */
@JsFun(code = "() => window.navigator.vendor")
private external fun getVendor(): String
