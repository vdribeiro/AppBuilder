package com.app.builder

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application

/** Entry point that launches the design showcase app in a desktop window. */
fun main() = application {
    Window(title = "Design Showcase", onCloseRequest = ::exitApplication) {
        App()
    }
}
