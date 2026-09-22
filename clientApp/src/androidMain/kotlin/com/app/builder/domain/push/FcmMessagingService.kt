package com.app.builder.domain.push

import com.app.builder.Application
import com.app.builder.core.telemetry.Telemetry
import com.app.builder.data.serializer.decode
import com.app.builder.domain.PushPayload
import com.app.builder.test.ExcludeFromTesting
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

/** Receives FCM messages and token refreshes. */
@ExcludeFromTesting
class FcmMessagingService: FirebaseMessagingService() {

    @Deprecated(message = "Use onRegistered(String) instead")
    override fun onNewToken(token: String) {
        AndroidPushProvider.setFcmToken(token = token)
    }

    override fun onMessageReceived(message: RemoteMessage) {
        val payload = message.toPushPayload() ?: run {
            Telemetry.error(tag = TAG, message = "Unable to decode FCM message payload")
            return
        }
        AndroidPushProvider.onPushPayloadReceived(payload = payload) {
            Application.apply { start(); await() }
        }
    }

    /**
     * Convert a [RemoteMessage] into a [PushPayload].
     *
     * @return The converted payload, or `null` if the message carries no [PushPayload.DATA_KEY] entry, or it cannot be decoded.
     */
    private fun RemoteMessage.toPushPayload(): PushPayload? =
        data[PushPayload.DATA_KEY]?.let { decode<PushPayload>(value = it) }

    companion object {
        private const val TAG = "FcmMessagingService"

        init {
            AndroidPushProvider.setFcmToken()
        }
    }
}
