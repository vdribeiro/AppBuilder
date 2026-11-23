package com.app.builder.ui.web

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * Displays the web page at [url].
 *
 * Links to a host other than the one of [url] or one of [allowedHosts] open outside the page. A web browser
 * does not apply this restriction.
 *
 * @param modifier The [Modifier] to be applied to the page.
 * @param url The address of the page.
 * @param javaScriptEnabled Whether the page can run JavaScript.
 * @param allowedHosts The hosts, besides the one of [url], that the page can navigate to.
 */
@Composable
expect fun WebPage(
    modifier: Modifier = Modifier,
    url: String,
    javaScriptEnabled: Boolean = false,
    allowedHosts: Set<String> = emptySet(),
)
