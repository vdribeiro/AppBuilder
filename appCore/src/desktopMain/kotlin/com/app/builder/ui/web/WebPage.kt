@file:ExcludeFromTesting

package com.app.builder.ui.web

import javafx.application.Platform
import javafx.embed.swing.JFXPanel
import javafx.scene.Scene
import javafx.scene.web.WebView
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.SwingPanel
import com.app.builder.test.ExcludeFromTesting

@Composable
actual fun WebPage(modifier: Modifier, url: String) {
    SwingPanel(
        modifier = modifier,
        factory = {
            // Keeps the JavaFX toolkit alive once this panel leaves the window
            Platform.setImplicitExit(false)
            JFXPanel().also { panel ->
                Platform.runLater {
                    panel.scene = Scene(WebView().apply { engine.load(url) })
                }
            }
        },
    )
}
