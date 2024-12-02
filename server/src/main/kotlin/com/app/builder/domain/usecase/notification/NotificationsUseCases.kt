package com.app.builder.domain.usecase.notification

import kotlin.uuid.Uuid
import kotlinx.coroutines.flow.Flow
import com.app.builder.domain.PushPayload

/** Defines the business workflows for managing notification. */
interface NotificationsUseCases {

    /**
     * Observes a continuous stream of notifications for a specific user from the database.
     *
     * @param userUuid The target user's UUID.
     * @return A Flow emitting lists of notifications.
     */
    fun observeNotifications(userUuid: Uuid): Flow<List<PushPayload>>

    /**
     * Persists a notification to the database.
     * This fires the notification observer to deal with the new notification.
     *
     * @param notification The notification to save.
     * @return `true` if the notification was saved successfully, `false` otherwise or no rows were affected.
     */
    suspend fun saveNotification(notification: PushPayload): Boolean

    /**
     * Deletes a notification in the database.
     *
     * @param notificationUuid The target notification UUID.
     * @return `true` if the notification was deleted successfully, `false` otherwise or no rows were affected.
     */
    suspend fun deleteNotification(notificationUuid: Uuid): Boolean
}
