package com.app.builder.ui.lifecycle

import androidx.compose.runtime.Composable

/**
 * Registers lifecycle callbacks for application background and foreground events.
 *
 * @param keys The keys used to trigger a restart of the lifecycle observers when it changes.
 * @param onBackground Callback executed when the application enters the background.
 * @param onForeground Callback executed when the application enters the foreground.
 * @param onDispose Callback executed when this composable leaves composition, after the lifecycle observer is removed.
 */
@Composable
expect fun Register(
    vararg keys: Any? = arrayOf(Unit),
    onBackground: () -> Unit = {},
    onForeground: () -> Unit = {},
    onDispose: () -> Unit = {},
)
