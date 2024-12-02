@file:ExcludeFromTesting

package com.app.builder

import platform.AVFAudio.AVAudioSession
import platform.AVFAudio.AVAudioSessionCategoryPlayback
import platform.AVFAudio.setActive
import platform.UIKit.UIViewController
import androidx.compose.ui.window.ComposeUIViewController
import com.app.builder.core.telemetry.Telemetry
import com.app.builder.test.ExcludeFromTesting
import com.app.builder.ui.App

private const val TAG = "App"

/**
 * Creates the root iOS view controller.
 * Starts the application and configures the audio session before hosting the Compose UI.
 *
 * @return A [UIViewController] hosting the Compose UI tree.
 */
@Suppress("FunctionName")
fun MainViewController(): UIViewController {
    Application.start()
    configureAudioSession()
    return ComposeUIViewController { App() }
}

/** Configures the audio session for playback. */
private fun configureAudioSession() {
    runCatching {
        AVAudioSession.sharedInstance().apply {
            setCategory(category = AVAudioSessionCategoryPlayback, error = null)
            setActive(active = true, error = null)
        }
    }.onFailure {
        Telemetry.error(tag = TAG, message = "Failed to set up audio session", throwable = it)
    }
}
