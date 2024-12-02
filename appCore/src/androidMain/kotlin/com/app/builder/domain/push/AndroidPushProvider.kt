package com.app.builder.domain.push

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import android.content.Intent
import com.app.builder.core.telemetry.Telemetry
import com.app.builder.data.serializer.decode
import com.app.builder.domain.PushPayload
import com.google.firebase.messaging.FirebaseMessaging

/** Provider to manage push payloads. */
object AndroidPushProvider {
    private const val TAG = "AndroidPushProvider"

    /** Scope with a specialized dispatcher limited to a single thread to act as a synchronization lock. */
    private val scope = CoroutineScope(context = Dispatchers.Default.limitedParallelism(parallelism = 1) + SupervisorJob())

    /**
     * Extracts and handles a notification payload from the intent's extras.
     *
     * @param intent The intent that started or was redelivered to the activity.
     */
    fun handleNotificationPayload(intent: Intent) {
        runCatching {
            val payload = intent.getStringExtra(PushPayload.DATA_KEY)?.let { decode<PushPayload>(value = it) } ?: error(message = "Unable to get payload from intent")
            PushProvider.handleNotificationClick(pushPayload = payload)
        }.onFailure {
            Telemetry.error(tag = TAG, message = "Unable to handle payload from intent", throwable = it)
        }
    }

    /** Sets [PushProvider] token from [FirebaseMessaging]. */
    fun setFcmToken() {
        scope.launch {
            @Suppress("DEPRECATION") val token = runCatching {
                FirebaseMessaging.getInstance().token.result
            }.onFailure {
                Telemetry.error(tag = TAG, message = "Unable to fetch FCM token", throwable = it)
            }.getOrNull()
            PushProvider.setToken(token = token)
        }
    }

    /**
     * Sets [PushProvider] token.
     *
     * @param token to set.
     */
    fun setFcmToken(token: String) {
        scope.launch {
            PushProvider.setToken(token = token)
        }
    }

    /**
     * Handles a [PushPayload].
     *
     * @param payload to handle.
     * @param init lambda to init the necessary services before processing the [payload].
     */
    fun onPushPayloadReceived(payload: PushPayload, init: suspend () -> Unit) {
        scope.launch {
            init()
            PushProvider.handlePushPayload(pushPayload = payload)
        }
    }
}
