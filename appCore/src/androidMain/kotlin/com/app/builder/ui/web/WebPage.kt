@file:ExcludeFromTesting

package com.app.builder.ui.web

import android.annotation.SuppressLint
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.net.toUri
import com.app.builder.test.ExcludeFromTesting

@SuppressLint("SetJavaScriptEnabled")
@Composable
actual fun WebPage(modifier: Modifier, url: String, javaScriptEnabled: Boolean, allowedHosts: Set<String>) {
    val uriHandler = LocalUriHandler.current
    AndroidView(
        modifier = modifier,
        factory = { context ->
            val hosts = allowedHosts + setOfNotNull(url.toUri().host)
            WebView(context).apply {
                webViewClient = object: WebViewClient() {
                    override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                        if (!request.isForMainFrame || request.url.host in hosts) return false
                        runCatching { uriHandler.openUri(uri = request.url.toString()) }
                        return true
                    }
                }
                settings.javaScriptEnabled = javaScriptEnabled
                settings.allowFileAccess = false
                settings.allowContentAccess = false
                loadUrl(url)
            }
        },
    )
}
