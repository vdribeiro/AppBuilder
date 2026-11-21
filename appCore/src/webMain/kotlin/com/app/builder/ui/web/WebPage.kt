@file:ExcludeFromTesting

package com.app.builder.ui.web

import kotlinx.browser.document
import org.w3c.dom.HTMLIFrameElement
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.HtmlElementView
import com.app.builder.test.ExcludeFromTesting

@Composable
actual fun WebPage(modifier: Modifier, url: String) {
    HtmlElementView(
        modifier = modifier,
        factory = {
            (document.createElement(localName = "iframe") as HTMLIFrameElement).apply {
                // Lets the frame load inside a cross-origin isolated page
                setAttribute(qualifiedName = "credentialless", value = "")
                style.border = "none"
                src = url
            }
        },
    )
}
