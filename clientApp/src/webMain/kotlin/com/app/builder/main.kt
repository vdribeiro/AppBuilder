@file:ExcludeFromTesting

package com.app.builder

import androidx.compose.ui.window.ComposeViewport
import com.app.builder.domain.push.FcmMessaging
import com.app.builder.test.ExcludeFromTesting
import com.app.builder.ui.App

/**
 * Web application entry point.
 * Mounts the Compose UI into the viewport.
 */
fun main() = ComposeViewport {
    Application.start()
    FcmMessaging.start()
    App()
}
