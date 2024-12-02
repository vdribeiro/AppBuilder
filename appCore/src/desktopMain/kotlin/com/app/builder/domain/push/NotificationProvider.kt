package com.app.builder.domain.push

import java.awt.SystemTray
import java.awt.TrayIcon
import java.awt.image.BufferedImage
import kotlin.concurrent.thread
import com.app.builder.core.telemetry.Telemetry
import com.app.builder.domain.PushPayload

actual object NotificationProvider {

    private const val TAG = "DesktopPushProvider"

    actual suspend fun showNotification(notification: PushPayload): Boolean = runCatching {
        val systemTray = getSystemTray() ?: return false
        val image = BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB)
        val trayIcon = TrayIcon(image, "Notification").apply {
            isImageAutoSize = true
        }.apply {
            addActionListener { PushProvider.handleNotificationClick(pushPayload = notification) }
        }
        systemTray.add(trayIcon)
        trayIcon.displayMessage(
            notification.title,
            notification.description,
            TrayIcon.MessageType.INFO
        )

        thread {
            Thread.sleep(5000)
            systemTray.remove(trayIcon)
        }
        true
    }.onFailure {
        Telemetry.error(tag = TAG, message = "Unable to show notification", throwable = it)
    }.getOrDefault(defaultValue = false)

    /**
     * Get the system tray safely.
     *
     * @return The [SystemTray], or null on error.
     */
    private fun getSystemTray(): SystemTray? = runCatching {
        if (!SystemTray.isSupported()) return null
        SystemTray.getSystemTray()
    }.onFailure {
        Telemetry.error(tag = TAG, message = "Unable to get system tray", throwable = it)
    }.getOrNull()
}
