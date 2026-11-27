package com.app.builder.core.locale

import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import platform.Foundation.NSCurrentLocaleDidChangeNotification
import platform.Foundation.NSLocale
import platform.Foundation.NSNotificationCenter
import platform.Foundation.preferredLanguages
import com.app.builder.core.platform.observe
import com.app.builder.core.telemetry.Telemetry

actual fun getDeviceLanguage(): String = runCatching {
    (NSLocale.preferredLanguages.first() as String).substringBefore(delimiter = "-").substringBefore(delimiter = "_")
}.onFailure {
    Telemetry.error(tag = TAG, message = "Unable to get language", throwable = it)
}.getOrDefault(defaultValue = DEFAULT_LANGUAGE)

actual fun observeDeviceLanguage(): Flow<String> = callbackFlow {
    trySend(element = getDeviceLanguage())
    val observer = NSNotificationCenter.defaultCenter.observe(
        name = NSCurrentLocaleDidChangeNotification
    ) { trySend(element = getDeviceLanguage()) }
    awaitClose { NSNotificationCenter.defaultCenter.removeObserver(observer) }
}.distinctUntilChanged()

private const val TAG = "Locale"
