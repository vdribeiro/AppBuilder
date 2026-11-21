package com.app.builder.ui.web

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * Displays the web page at [url].
 *
 * @param modifier The [Modifier] to be applied to the page.
 * @param url The address of the page.
 */
@Composable
expect fun WebPage(modifier: Modifier = Modifier, url: String)
