@file:ExcludeFromTesting

package com.app.builder.ui.web

import java.net.URI
import javafx.application.Platform
import javafx.embed.swing.JFXPanel
import javafx.scene.Scene
import javafx.scene.web.WebView
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.SwingPanel
import androidx.compose.ui.platform.LocalUriHandler
import com.app.builder.test.ExcludeFromTesting

@Composable
actual fun WebPage(modifier: Modifier, url: String, javaScriptEnabled: Boolean, allowedHosts: Set<String>) {
    val uriHandler = LocalUriHandler.current
    SwingPanel(
        modifier = modifier,
        factory = {
            val hosts = allowedHosts + setOfNotNull(hostOf(address = url))
            // Keeps the JavaFX toolkit alive once this panel leaves the window
            Platform.setImplicitExit(false)
            JFXPanel().also { panel ->
                Platform.runLater {
                    panel.scene = Scene(
                        WebView().apply {
                            engine.isJavaScriptEnabled = javaScriptEnabled
                            engine.locationProperty().addListener { _, _, location ->
                                if (hostOf(address = location) !in hosts) {
                                    Platform.runLater { engine.loadWorker.cancel() }
                                    runCatching { uriHandler.openUri(uri = location) }
                                }
                            }
                            engine.load(url)
                        },
                    )
                }
            }
        },
    )
}

/**
 * Returns the host of [address], or null when it has none.
 */
private fun hostOf(address: String): String? = runCatching { URI(address).host }.getOrNull()
