package com.app.builder.core.locale

import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import com.app.builder.applicationContext
import com.app.builder.core.telemetry.Telemetry

actual fun getDeviceLanguage(): String = runCatching {
    applicationContext.resources.configuration.locales[0].language.ifBlank { DEFAULT_LANGUAGE }
}.onFailure {
    Telemetry.error(tag = TAG, message = "Unable to get language", throwable = it)
}.getOrDefault(defaultValue = DEFAULT_LANGUAGE)

actual fun observeDeviceLanguage(): Flow<String> = callbackFlow {
    trySend(element = getDeviceLanguage())
    val receiver = object: BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == Intent.ACTION_LOCALE_CHANGED) trySend(element = getDeviceLanguage())
        }
    }

    runCatching {
        applicationContext.registerReceiver(receiver, IntentFilter(Intent.ACTION_LOCALE_CHANGED))
    }.onFailure {
        Telemetry.error(tag = TAG, message = "Unable to register observer for locale changes", throwable = it)
        close(cause = it)
        return@callbackFlow
    }

    awaitClose {
        runCatching {
            applicationContext.unregisterReceiver(receiver)
        }.onFailure {
            Telemetry.error(tag = TAG, message = "Unable to unregister observer for locale changes", throwable = it)
        }
    }
}.distinctUntilChanged()

private const val TAG = "Locale"
