@file:ExcludeFromTesting

package com.app.builder.ui.web

import kotlinx.cinterop.readValue
import platform.CoreGraphics.CGRectZero
import platform.Foundation.NSURL
import platform.Foundation.NSURLRequest
import platform.WebKit.WKNavigationAction
import platform.WebKit.WKNavigationActionPolicy
import platform.WebKit.WKNavigationDelegateProtocol
import platform.WebKit.WKWebView
import platform.WebKit.WKWebViewConfiguration
import platform.darwin.NSObject
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.UriHandler
import androidx.compose.ui.viewinterop.UIKitView
import com.app.builder.test.ExcludeFromTesting

@Composable
actual fun WebPage(modifier: Modifier, url: String, javaScriptEnabled: Boolean, allowedHosts: Set<String>) {
    val uriHandler = LocalUriHandler.current
    // The web view holds its delegate weakly
    val delegate = remember {
        NavigationDelegate(
            hosts = allowedHosts + setOfNotNull(NSURL.URLWithString(URLString = url)?.host),
            uriHandler = uriHandler,
        )
    }
    UIKitView(
        modifier = modifier,
        factory = {
            val configuration = WKWebViewConfiguration().apply {
                defaultWebpagePreferences.allowsContentJavaScript = javaScriptEnabled
            }
            WKWebView(frame = CGRectZero.readValue(), configuration = configuration).apply {
                navigationDelegate = delegate
                NSURL.URLWithString(URLString = url)?.let { loadRequest(request = NSURLRequest.requestWithURL(URL = it)) }
            }
        },
    )
}

/**
 * Keeps main frame navigation inside [hosts] and opens any other address with [uriHandler].
 */
private class NavigationDelegate(
    private val hosts: Set<String>,
    private val uriHandler: UriHandler,
): NSObject(), WKNavigationDelegateProtocol {

    override fun webView(
        webView: WKWebView,
        decidePolicyForNavigationAction: WKNavigationAction,
        decisionHandler: (WKNavigationActionPolicy) -> Unit,
    ) {
        val address = decidePolicyForNavigationAction.request.URL
        if (decidePolicyForNavigationAction.targetFrame?.mainFrame == false || address?.host in hosts) {
            decisionHandler(WKNavigationActionPolicy.WKNavigationActionPolicyAllow)
            return
        }
        address?.absoluteString?.let { runCatching { uriHandler.openUri(uri = it) } }
        decisionHandler(WKNavigationActionPolicy.WKNavigationActionPolicyCancel)
    }
}
