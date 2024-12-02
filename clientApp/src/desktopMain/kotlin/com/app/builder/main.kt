@file:ExcludeFromTesting

package com.app.builder

import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.painterResource
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPlacement
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import appbuilder.clientapp.generated.resources.Res
import appbuilder.clientapp.generated.resources.ic_launcher
import com.app.builder.core.telemetry.Telemetry
import com.app.builder.test.ExcludeFromTesting
import com.app.builder.ui.App
import com.app.builder.ui.lifecycle.LocalWindowState
import javafx.embed.swing.JFXPanel

private const val TAG = "App"

/**
 * Lazily initializes the JavaFX toolkit by instantiating a [JFXPanel], logging and swallowing any failure.
 * `true` if JavaFX started successfully, `false` otherwise.
 */
private val initializeJfx by lazy {
    runCatching {
        JFXPanel()
        true
    }.onFailure {
        Telemetry.error(tag = TAG, message = "Unable to start JavaFX", throwable = it)
    }.getOrDefault(defaultValue = false)
}

/**
 * Desktop application entry point.
 * Starts the application and opens the maximized main window hosting the Compose UI.
 */
fun main() = application {
    Application.start()
    Telemetry.info(tag = TAG, message = "JavaFX = $initializeJfx")

    val windowState = rememberWindowState(placement = WindowPlacement.Maximized)
    val icon = painterResource(resource = Res.drawable.ic_launcher)
    val coroutineScope = rememberCoroutineScope()

    Window(
        title = AppInfo.NAME,
        state = windowState,
        icon = icon,
        onCloseRequest = {
            coroutineScope.launch {
                Application.stop()
                exitApplication()
            }
        },
    ) {
        val compositionValues = remember(key1 = windowState) { listOf(element = LocalWindowState provides windowState) }
        App(compositionValues = compositionValues)
    }
}
