package com.app.builder.domain.push

import com.app.builder.Application

/** FCM push delegate. */
@Suppress("unused")
object FcmMessaging {

    /**
     * Decodes a remote push's `payload` data field unless it's a visible payload.
     *
     * @param userInfo The notification's raw `userInfo` dictionary.
     * @param onResult Invoked with whether new data was fetched, so Swift can call the real `UIBackgroundFetchResult` completion handler.
     */
    fun onPushPayloadReceived(userInfo: Map<Any?, *>, onResult: (Boolean) -> Unit) {
        ApplePushProvider.onPushPayloadReceived(userInfo = userInfo, onResult = onResult) {
            Application.apply { start(); await() }
        }
    }
}