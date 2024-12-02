package com.app.builder.domain.push

import kotlin.js.Promise
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.await
import kotlinx.coroutines.launch
import com.app.builder.WebPushConfig
import com.app.builder.core.flow.Dispatcher
import com.app.builder.core.telemetry.Telemetry
import com.app.builder.data.serializer.decode
import com.app.builder.domain.PushPayload

/** Provider to manage push payloads. */
object WebPushProvider {

    private const val TAG = "WebPushProvider"

    /** Observe FCM tokens. */
    suspend fun observeToken() {
        val token = runCatching {
            ensureFirebaseInitialized().await()
            getFcmWebToken(vapidKey = WebPushConfig.VAPID_KEY).await().toString().takeIf { it.isNotEmpty() }
        }.onFailure {
            Telemetry.error(tag = TAG, message = "Unable to fetch FCM web token", throwable = it)
        }.getOrNull()
        PushProvider.setToken(token = token)
    }

    /**
     * Observe FCM messages.
     *
     * @param scope to handle payloads.
     */
    suspend fun observeMessages(scope: CoroutineScope) {
        runCatching {
            ensureFirebaseInitialized().await()
            onFcmWebMessage { json ->
                scope.launch(context = Dispatcher.Default) {
                    val payload = decode<PushPayload>(value = json) ?: run {
                        Telemetry.error(tag = TAG, message = "Unable to decode message payload")
                        return@launch
                    }
                    PushProvider.handlePushPayload(pushPayload = payload)
                }
            }
        }.onFailure {
            Telemetry.error(tag = TAG, message = "Unable to observe FCM web messages", throwable = it)
        }
    }
}

/**
 * Lazily initializes the Firebase app and messaging SDK, caching the instances on `window.__abFirebase`.
 *
 * @return A [Promise] that resolves once Firebase is ready to use.
 */
@JsFun(
    code = """
    async () => {
        if (window.__abFirebase) return;
        const appMod = await import('firebase/app');
        const msgMod = await import('firebase/messaging');
        const app = appMod.initializeApp(""" + WebPushConfig.FIREBASE_CONFIG_JSON + """);
        window.__abFirebase = { app: app, messaging: msgMod.getMessaging(app), msgMod: msgMod };
    }
"""
)
private external fun ensureFirebaseInitialized(): Promise<JsAny?>

/**
 * Requests notification permission, registers the Firebase messaging service worker, and fetches the FCM web token.
 *
 * @param vapidKey The VAPID public key used to authorize the token request.
 * @return A [Promise] resolving to the FCM token, or an empty string if permission was denied or the browser lacks support.
 */
@JsFun(
    code = """
    async (vapidKey) => {
        if (!('serviceWorker' in navigator) || !('Notification' in window)) return "";
        const permission = await Notification.requestPermission();
        if (permission !== 'granted') return "";
        const registration = await navigator.serviceWorker.register('/firebase-messaging-sw.js');
        const firebase = window.__abFirebase;
        const token = await firebase.msgMod.getToken(firebase.messaging, { vapidKey: vapidKey, serviceWorkerRegistration: registration });
        return token || "";
    }
"""
)
private external fun getFcmWebToken(vapidKey: String): Promise<JsString>

/**
 * Registers [callback] to be invoked with the raw JSON payload string of each foreground FCM message.
 *
 * @param callback Invoked with the message's `data.payload` JSON string.
 */
@JsFun(
    code = """
    (callback) => {
        const firebase = window.__abFirebase;
        firebase.msgMod.onMessage(firebase.messaging, (payload) => {
            const data = payload && payload.data && payload.data.payload ? payload.data.payload : null;
            if (data) callback(data);
        });
    }
"""
)
private external fun onFcmWebMessage(callback: (String) -> Unit)
