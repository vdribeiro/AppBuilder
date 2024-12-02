package com.app.builder.ui.push

import kotlinx.coroutines.launch
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import com.app.builder.core.platform.OS
import com.app.builder.core.platform.platform
import com.app.builder.domain.push.BroadcastService
import com.app.builder.domain.push.PushService
import com.app.builder.ui.lifecycle.Register

/**
 * Pauses the SSE broadcast connection while the app is backgrounded, resuming it on foreground.
 * Only active on platforms where the OS suspends background sockets and Firebase Cloud Messaging covers background delivery.
 *
 * @param broadcastService The service responsible for broadcasting push notifications.
 */
@Composable
fun RegisterBroadcastLifecycle(broadcastService: BroadcastService?) {
    when (platform.os) {
        OS.Android, OS.Ios -> Unit
        OS.Windows, OS.Mac, OS.Linux, OS.Web, OS.Unknown -> return
    }
    val coroutineScope = rememberCoroutineScope()
    Register(
        broadcastService,
        onBackground = {
            coroutineScope.launch {
                broadcastService?.stop()
            }
        },
        onForeground = {
            broadcastService?.start()
        },
        onDispose = {
            coroutineScope.launch {
                broadcastService?.stop()
            }
        }
    )
}

/**
 * Pauses the WebSocket push connection while the app is backgrounded, resuming it on foreground.
 * Only active on platforms where the OS suspends background sockets and Firebase Cloud Messaging covers background delivery.
 *
 * @param pushService The service responsible for sending push notifications.
 */
@Composable
fun RegisterPushLifecycle(pushService: PushService?) {
    when (platform.os) {
        OS.Android, OS.Ios -> Unit
        OS.Windows, OS.Mac, OS.Linux, OS.Web, OS.Unknown -> return
    }
    val coroutineScope = rememberCoroutineScope()
    Register(
        pushService,
        onBackground = {
            coroutineScope.launch {
                pushService?.stop()
            }
        },
        onForeground = {
            pushService?.start()
        },
        onDispose = {
            coroutineScope.launch {
                pushService?.stop()
            }
        }
    )
}
