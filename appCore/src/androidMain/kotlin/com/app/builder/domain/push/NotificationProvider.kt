package com.app.builder.domain.push

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.annotation.RequiresPermission
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.app.builder.applicationContext
import com.app.builder.core.telemetry.Telemetry
import com.app.builder.data.serializer.encode
import com.app.builder.domain.PushPayload
import android.app.Notification as OSNotification

actual object NotificationProvider {

    private const val TAG = "NotificationProvider"

    /** Notifications general channel. */
    private val channel: String = "general".also { createChannel(channelId = it) }

    /**
     * Checks whether the app may post notifications, which only requires a runtime grant from Android 13 onwards.
     *
     * @return `true` if notifications can be posted, `false` otherwise.
     */
    private fun hasPermission(): Boolean =
        when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> ContextCompat.checkSelfPermission(applicationContext, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
            else -> true
        }

    @RequiresPermission(value = Manifest.permission.POST_NOTIFICATIONS)
    actual suspend fun showNotification(notification: PushPayload): Boolean = runCatching {
        val notificationManager = getNotificationManager()
        if (notificationManager == null || !notificationManager.areNotificationsEnabled() || !hasPermission()) return false
        val notificationBuilder = OSNotification.Builder(applicationContext, channel).apply {
            setContentTitle(notification.title)
            setContentText(notification.description)
            setSmallIcon(applicationContext.applicationInfo.icon)
            setAutoCancel(true)
            getIntent(notification = notification)?.let { setContentIntent(it) }
        }
        notificationManager.notify(notification.uuid.hashCode(), notificationBuilder.build())
        true
    }.onFailure {
        Telemetry.error(tag = TAG, message = "Unable to show notification", throwable = it)
    }.getOrDefault(defaultValue = false)

    /**
     * Get the notification manager safely.
     *
     * @return The [NotificationManagerCompat], or null on error.
     */
    private fun getNotificationManager(): NotificationManagerCompat? = runCatching {
        NotificationManagerCompat.from(applicationContext)
    }.onFailure {
        Telemetry.error(tag = TAG, message = "Unable to get notification manager", throwable = it)
    }.getOrNull()

    /**
     * Create a notification channel.
     *
     * @param channelId The id and user visible name of the channel. Must be unique.
     * @return true if the channel was successfully created, false otherwise.
     */
    private fun createChannel(channelId: String): Boolean = runCatching {
        val notificationManager = getNotificationManager()
        if (notificationManager == null || !notificationManager.areNotificationsEnabled() || !hasPermission()) return false
        val channel = NotificationChannel(channelId, channelId.take(n = 1).uppercase(), NotificationManager.IMPORTANCE_DEFAULT)
        notificationManager.createNotificationChannel(channel)
        true
    }.onFailure {
        Telemetry.error(tag = TAG, message = "Unable to create notification channel $channelId", throwable = it)
    }.getOrDefault(defaultValue = false)

    /**
     * Get the notification intent.
     *
     * @param notification The notification data.
     * @return The [PendingIntent], or null on error.
     */
    private fun getIntent(notification: PushPayload): PendingIntent? = runCatching {
        val payload = encode(value = notification) ?: error(message = "Unable to encode push payload ${notification.uuid}")
        applicationContext.packageManager.getLaunchIntentForPackage(applicationContext.packageName)?.apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(PushPayload.DATA_KEY, payload)
        }?.let { intent ->
            PendingIntent.getActivity(
                applicationContext,
                notification.uuid.hashCode(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        }
    }.onFailure {
        Telemetry.error(tag = TAG, message = "Unable to get intent for notification", throwable = it)
    }.getOrNull()
}
