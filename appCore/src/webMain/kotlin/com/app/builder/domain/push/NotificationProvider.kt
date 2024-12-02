package com.app.builder.domain.push

import kotlinx.browser.window
import com.app.builder.core.telemetry.Telemetry
import com.app.builder.domain.PushPayload
import org.w3c.notifications.DENIED
import org.w3c.notifications.GRANTED
import org.w3c.notifications.NotificationOptions
import org.w3c.notifications.NotificationPermission
import org.w3c.notifications.Notification as OSNotification

actual object NotificationProvider {

    private const val TAG = "NotificationProvider"

    actual suspend fun showNotification(notification: PushPayload): Boolean = runCatching {
        val permission = OSNotification.permission
        when {
            permission == NotificationPermission.GRANTED -> show(notification = notification)
            permission != NotificationPermission.DENIED -> OSNotification.requestPermission {
                if (it == NotificationPermission.GRANTED) show(notification = notification)
            }
        }
        true
    }.onFailure {
        Telemetry.error(tag = TAG, message = "Unable to show notification", throwable = it)
    }.getOrDefault(defaultValue = false)

    /**
     * Show the notification.
     *
     * @param notification The notification data.
     * @return True if the notification was shown, false otherwise.
     */
    private fun show(notification: PushPayload): Boolean = runCatching {
        val options = NotificationOptions(body = notification.description)
        OSNotification(title = notification.title, options = options).onclick = {
            PushProvider.handleNotificationClick(pushPayload = notification)
            window.focus()
        }
        true
    }.onFailure {
        Telemetry.error(tag = TAG, message = "Unable to show notification", throwable = it)
    }.getOrDefault(defaultValue = false)
}
