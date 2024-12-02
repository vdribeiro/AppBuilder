package com.app.builder.domain.push

import kotlinx.coroutines.withContext
import com.app.builder.core.flow.Dispatcher
import com.app.builder.core.telemetry.Telemetry
import com.app.builder.data.serializer.encode
import com.app.builder.domain.PushPayload
import com.google.firebase.messaging.ApnsConfig
import com.google.firebase.messaging.Aps
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.Message
import com.google.firebase.messaging.MessagingErrorCode
import com.google.firebase.messaging.MulticastMessage
import com.google.firebase.messaging.Notification

/**
 * Sends push payloads via the Firebase Admin SDK.
 *
 * @property firebaseMessaging The Firebase Cloud Messaging client.
 */
class FcmManager(
    private val firebaseMessaging: FirebaseMessaging
): FcmService {

    override suspend fun subscribeToBroadcastTopic(token: String): Boolean = withContext(context = Dispatcher.IO) {
        runCatching {
            firebaseMessaging.subscribeToTopic(listOf(token), BROADCAST_TOPIC)
            true
        }.onFailure {
            Telemetry.error(tag = TAG, message = "Unable to subscribe device token to broadcast topic", throwable = it)
        }.getOrDefault(defaultValue = false)
    }

    override suspend fun sendToBroadcastTopic(pushPayload: PushPayload): Boolean = withContext(context = Dispatcher.IO) {
        runCatching {
            val message = pushPayload.toMessageBuilder().setTopic(BROADCAST_TOPIC).build()
            firebaseMessaging.send(message)
            true
        }.onFailure {
            Telemetry.error(tag = TAG, message = "Unable to send FCM broadcast message", throwable = it)
        }.getOrDefault(defaultValue = false)
    }

    override suspend fun sendToTokens(tokens: List<String>, pushPayload: PushPayload): List<String> = withContext(context = Dispatcher.IO) {
        runCatching {
            if (tokens.isEmpty()) return@withContext emptyList()
            @Suppress("DEPRECATION") val message = pushPayload.toMulticastMessageBuilder().addAllTokens(tokens).build()
            firebaseMessaging.sendEachForMulticast(message)
        }.onFailure {
            Telemetry.error(tag = TAG, message = "Unable to send FCM multicast message", throwable = it)
        }.getOrNull()?.responses.orEmpty().mapIndexedNotNull { index, response ->
            val errorCode = response.exception?.messagingErrorCode
            if (!response.isSuccessful && errorCode == MessagingErrorCode.UNREGISTERED) tokens[index] else null
        }
    }

    /**
     * Builds a single-recipient FCM [Message] carrying the encoded payload as data, plus a visible notification block or a silent APNs flag.
     *
     * @receiver The payload to build a message for.
     * @return The configured message builder.
     */
    private fun PushPayload.toMessageBuilder(): Message.Builder {
        val builder = Message.builder().putData(PushPayload.DATA_KEY, encode(value = this).orEmpty())
        applyNotificationAndApns(builder = builder)
        return builder
    }

    /**
     * Builds a multicast FCM [MulticastMessage] carrying the encoded payload as data, plus a visible notification block or a silent APNs flag.
     *
     * @receiver The payload to build a message for.
     * @return The configured message builder.
     */
    private fun PushPayload.toMulticastMessageBuilder(): MulticastMessage.Builder {
        val builder = MulticastMessage.builder().putData(PushPayload.DATA_KEY, encode(value = this).orEmpty())
        applyNotificationAndApns(builder = builder)
        return builder
    }

    /** Adds a visible notification block for user-facing payloads, or marks silent/data-only payloads for background delivery on iOS via APNs' `content-available` flag. */
    private fun PushPayload.applyNotificationAndApns(builder: Any) {
        val title = (this as? PushPayload.Notification)?.title ?: (this as? PushPayload.EntityNotification)?.title
        val description = (this as? PushPayload.Notification)?.description ?: (this as? PushPayload.EntityNotification)?.description
        val notification = Notification
            .builder()
            .apply {
                if (title != null) setTitle(title)
                if (description != null) setBody(description)
            }
            .build()
        val apnsConfig = ApnsConfig.builder()
            .setAps(Aps.builder().setContentAvailable(true).build())
            .build()

        when (builder) {
            is Message.Builder -> {
                builder.setNotification(notification)
                builder.setApnsConfig(apnsConfig)
            }

            is MulticastMessage.Builder -> {
                builder.setNotification(notification)
                builder.setApnsConfig(apnsConfig)
            }
        }
    }

    companion object {
        private const val TAG = "FcmManager"

        /** The FCM topic every device subscribes to in order to receive broadcast payloads. */
        private const val BROADCAST_TOPIC = "broadcast"
    }
}
