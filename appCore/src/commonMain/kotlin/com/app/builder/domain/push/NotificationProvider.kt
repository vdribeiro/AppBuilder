package com.app.builder.domain.push

import com.app.builder.domain.PushPayload

/** Provider to manage notifications intended to be extended by platform-specific implementations. */
expect object NotificationProvider {

    /**
     * Triggers a native system notification.
     *
     * @param notification The notification to be shown.
     * @return `true` if the notification was successfully shown, `false` otherwise.
     */
    suspend fun showNotification(notification: PushPayload): Boolean
}
