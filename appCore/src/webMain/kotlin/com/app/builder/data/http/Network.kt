package com.app.builder.data.http

import kotlinx.browser.window
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import com.app.builder.core.telemetry.Telemetry
import org.w3c.dom.events.Event

actual fun isInternetAvailable(): Boolean = runCatching {
    window.navigator.onLine
}.onFailure {
    Telemetry.error(tag = TAG, message = "Unable to check internet connection", throwable = it)
}.getOrDefault(defaultValue = false)

actual fun getInternetAvailabilityFlow(): Flow<Boolean> = callbackFlow {
    trySend(element = isInternetAvailable())

    val onlineListener: (Event) -> Unit = { trySend(element = true) }
    val offlineListener: (Event) -> Unit = { trySend(element = false) }

    window.addEventListener(type = "online", callback = onlineListener)
    window.addEventListener(type = "offline", callback = offlineListener)

    awaitClose {
        window.removeEventListener(type = "online", callback = onlineListener)
        window.removeEventListener(type = "offline", callback = offlineListener)
    }
}.distinctUntilChanged()

private const val TAG = "Network"