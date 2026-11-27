package com.app.builder.core.locale

import kotlinx.browser.window
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import org.w3c.dom.events.Event
import com.app.builder.core.telemetry.Telemetry

actual fun getDeviceLanguage(): String = runCatching {
    window.navigator.language.substringBefore(delimiter = "-").ifBlank { DEFAULT_LANGUAGE }
}.onFailure {
    Telemetry.error(tag = TAG, message = "Unable to get language", throwable = it)
}.getOrDefault(defaultValue = DEFAULT_LANGUAGE)

actual fun observeDeviceLanguage(): Flow<String> = callbackFlow {
    trySend(element = getDeviceLanguage())
    val listener: ((Event) -> Unit) = { trySend(element = getDeviceLanguage()) }
    window.addEventListener(type = "languagechange", callback = listener)
    awaitClose { window.removeEventListener(type = "languagechange", callback = listener) }
}.distinctUntilChanged()

private const val TAG = "Locale"
