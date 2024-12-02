package com.app.builder.ui.lifecycle

import kotlinx.browser.document
import kotlinx.browser.window
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import org.w3c.dom.events.Event

@Composable
actual fun Register(
    vararg keys: Any?,
    onBackground: () -> Unit,
    onForeground: () -> Unit,
    onDispose: () -> Unit,
) {
    DisposableEffect(keys = keys) {
        val visibilityListener: (Event) -> Unit = {
            if (isHidden()) {
                onBackground()
            } else {
                onForeground()
            }
        }

        val focusListener: (Event) -> Unit = { onForeground() }
        val blurListener: (Event) -> Unit = { onBackground() }

        document.addEventListener(type = "visibilitychange", callback = visibilityListener)
        window.addEventListener(type = "focus", callback = focusListener)
        window.addEventListener(type = "blur", callback = blurListener)

        onDispose {
            document.removeEventListener(type = "visibilitychange", callback = visibilityListener)
            window.removeEventListener(type = "focus", callback = focusListener)
            window.removeEventListener(type = "blur", callback = blurListener)
            onDispose()
        }
    }
}

/**
 * Checks whether the document is currently hidden (e.g. tab backgrounded or minimized).
 *
 * @return `true` if the document is hidden, `false` otherwise.
 */
@JsFun(code = "() => document.hidden")
private external fun isHidden(): Boolean
