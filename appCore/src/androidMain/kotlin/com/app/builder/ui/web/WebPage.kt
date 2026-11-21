@file:ExcludeFromTesting

package com.app.builder.ui.web

import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.app.builder.test.ExcludeFromTesting

@Composable
actual fun WebPage(modifier: Modifier, url: String) {
    AndroidView(
        modifier = modifier,
        factory = { context ->
            WebView(context).apply {
                // Keeps link navigation inside this view
                webViewClient = WebViewClient()
                settings.javaScriptEnabled = true
                loadUrl(url)
            }
        },
    )
}
