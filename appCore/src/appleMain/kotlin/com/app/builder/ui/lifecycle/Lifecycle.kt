package com.app.builder.ui.lifecycle

import platform.Foundation.NSNotificationCenter
import platform.UIKit.UIApplicationDidBecomeActiveNotification
import platform.UIKit.UIApplicationWillResignActiveNotification
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import com.app.builder.core.platform.observe

@Composable
actual fun Register(
    vararg keys: Any?,
    onBackground: () -> Unit,
    onForeground: () -> Unit,
    onDispose: () -> Unit,
) {
    DisposableEffect(keys = keys) {
        onForeground()
        val pauseObserver = NSNotificationCenter.defaultCenter.observe(
            name = UIApplicationWillResignActiveNotification,
            onObserve = onBackground
        )
        val resumeObserver = NSNotificationCenter.defaultCenter.observe(
            name = UIApplicationDidBecomeActiveNotification,
            onObserve = onForeground
        )

        onDispose {
            NSNotificationCenter.defaultCenter.removeObserver(observer = pauseObserver)
            NSNotificationCenter.defaultCenter.removeObserver(observer = resumeObserver)
            onDispose()
        }
    }
}
