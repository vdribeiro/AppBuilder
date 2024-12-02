package com.app.builder.ui.lifecycle

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.window.WindowState

/**
 * CompositionLocal providing the current window's [WindowState].
 * Must be provided by the application entry point where the [androidx.compose.ui.window.Window] is created.
 */
val LocalWindowState: ProvidableCompositionLocal<WindowState> = staticCompositionLocalOf { WindowState() }

@Composable
actual fun Register(
    vararg keys: Any?,
    onBackground: () -> Unit,
    onForeground: () -> Unit,
    onDispose: () -> Unit,
) {
    val lifecycleOwner = LocalWindowState.current
    DisposableEffect(*keys, lifecycleOwner.isMinimized) {
        if (lifecycleOwner.isMinimized) onBackground() else onForeground()

        onDispose { onDispose() }
    }
}
