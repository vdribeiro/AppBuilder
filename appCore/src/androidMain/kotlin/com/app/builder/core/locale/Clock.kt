package com.app.builder.core.locale

import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import com.app.builder.applicationContext
import com.app.builder.core.telemetry.Telemetry

actual fun observeClockChanges(): Flow<Unit> = callbackFlow {
    val receiver = object: BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            trySend(element = Unit)
        }
    }

    runCatching {
        applicationContext.registerReceiver(receiver, IntentFilter(Intent.ACTION_TIME_CHANGED))
    }.onFailure {
        Telemetry.error(tag = TAG, message = "Unable to register observer for clock changes", throwable = it)
        close(cause = it)
        return@callbackFlow
    }
    awaitClose {
        runCatching {
            applicationContext.unregisterReceiver(receiver)
        }.onFailure {
            Telemetry.error(tag = TAG, message = "Unable to unregister observer for clock changes", throwable = it)
        }
    }
}

private const val TAG = "Clock"
