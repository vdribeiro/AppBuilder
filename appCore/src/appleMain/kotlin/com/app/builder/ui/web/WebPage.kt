@file:ExcludeFromTesting

package com.app.builder.ui.web

import platform.Foundation.NSURL
import platform.Foundation.NSURLRequest
import platform.WebKit.WKWebView
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.UIKitView
import com.app.builder.test.ExcludeFromTesting

@Composable
actual fun WebPage(modifier: Modifier, url: String) {
    UIKitView(
        modifier = modifier,
        factory = {
            WKWebView().apply {
                NSURL.URLWithString(URLString = url)?.let { loadRequest(request = NSURLRequest.requestWithURL(URL = it)) }
            }
        },
    )
}
