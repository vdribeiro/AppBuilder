package com.app.builder.core.platform

import kotlinx.browser.window

actual val developmentMode: Boolean
    get() = runCatching {
        with(receiver = window.location) {
            search.contains(other = "debug=true") || hostname == "localhost" || hostname == "127.0.0.1"
        }
    }.getOrDefault(defaultValue = false)
