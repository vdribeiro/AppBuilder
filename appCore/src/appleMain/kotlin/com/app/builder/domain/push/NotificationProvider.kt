package com.app.builder.domain.push

import kotlin.coroutines.resume
import kotlinx.coroutines.suspendCancellableCoroutine
import platform.UserNotifications.UNAuthorizationOptionAlert
import platform.UserNotifications.UNAuthorizationOptionSound
import platform.UserNotifications.UNMutableNotificationContent
import platform.UserNotifications.UNNotificationRequest
import platform.UserNotifications.UNNotificationSound
import platform.UserNotifications.UNUserNotificationCenter
import com.app.builder.core.telemetry.Telemetry
import com.app.builder.data.serializer.encode
import com.app.builder.domain.PushPayload

actual object NotificationProvider {

    private const val TAG = "NotificationProvider"

    actual suspend fun showNotification(notification: PushPayload): Boolean = runCatching {
        val notificationCenter = getNotificationCenter() ?: return false
        suspendCancellableCoroutine { continuation ->
            notificationCenter.requestAuthorizationWithOptions(UNAuthorizationOptionAlert or UNAuthorizationOptionSound) { granted, _ ->
                if (!granted) {
                    Telemetry.error(tag = TAG, message = "Authorization not granted.")
                    continuation.resume(value = false)
                    return@requestAuthorizationWithOptions
                }
                val payload = encode(value = notification) ?: run {
                    Telemetry.error(tag = TAG, message = "Unable to decode payload ${notification.uuid}.")
                    continuation.resume(value = false)
                    return@requestAuthorizationWithOptions
                }
                val content = UNMutableNotificationContent().apply {
                    setTitle(title = notification.title)
                    setBody(body = notification.description)
                    setSound(sound = UNNotificationSound.defaultSound)
                    setUserInfo(userInfo = mapOf(PushPayload.DATA_KEY to payload))
                }
                val request = UNNotificationRequest.requestWithIdentifier(
                    identifier = notification.uuid.toString(),
                    content = content,
                    trigger = null
                )
                notificationCenter.addNotificationRequest(request = request) { error ->
                    if (error != null) Telemetry.error(tag = TAG, message = "Failed to show notification: ${error.localizedDescription}")
                    continuation.resume(value = error == null)
                }
            }
        }
    }.onFailure {
        Telemetry.error(tag = TAG, message = "Unable to show notification", throwable = it)
    }.getOrDefault(defaultValue = false)

    /**
     * Get the notification center safely.
     *
     * @return The [UNUserNotificationCenter], or null on error.
     */
    private fun getNotificationCenter(): UNUserNotificationCenter? = runCatching {
        UNUserNotificationCenter.currentNotificationCenter()
    }.onFailure {
        Telemetry.error(tag = TAG, message = "Unable to get notification center", throwable = it)
    }.getOrNull()
}
