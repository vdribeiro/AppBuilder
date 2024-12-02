package com.app.builder.domain.usecase.notification

import kotlin.uuid.Uuid
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.withContext
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.r2dbc.R2dbcDatabase
import org.jetbrains.exposed.v1.r2dbc.deleteWhere
import org.jetbrains.exposed.v1.r2dbc.selectAll
import org.jetbrains.exposed.v1.r2dbc.upsert
import com.app.builder.core.flow.Dispatcher
import com.app.builder.core.telemetry.Telemetry
import com.app.builder.data.database.safeTransaction
import com.app.builder.data.database.table.NotificationTable
import com.app.builder.data.signal.InstanceSignal
import com.app.builder.domain.PushPayload
import com.app.builder.toEnumOrNull

/**
 * Gateway implementation for notifications use cases.
 *
 * @property database The database instance used to execute transactions.
 * @property instanceSignal Signals new notifications to observers on every server instance.
 */
class NotificationsGateway(
    private val database: R2dbcDatabase,
    private val instanceSignal: InstanceSignal
): NotificationsUseCases {

    override fun observeNotifications(userUuid: Uuid): Flow<List<PushPayload>> = flow {
        emit(value = getNotifications(userUuid))
        instanceSignal
            .observe(channel = NOTIFICATION_CHANNEL)
            .filter { targetUserUuid -> targetUserUuid == userUuid.toString() }
            .collect { emit(value = getNotifications(userUuid)) }
    }.flowOn(context = Dispatcher.IO)

    override suspend fun saveNotification(notification: PushPayload): Boolean = withContext(context = Dispatcher.IO) {
        if (notification !is PushPayload.EntityNotification) {
            Telemetry.error(tag = TAG, message = "Unable to persist notification ${notification.uuid}. Only entity notifications can be persisted")
            return@withContext false
        }
        val notificationUserUuid = notification.userUuid
        database.safeTransaction {
            NotificationTable.upsert {
                it[uuid] = notification.uuid
                it[userUuid] = notificationUserUuid
                it[title] = notification.title
                it[description] = notification.description
                it[entityType] = notification.entityType.name
                it[entityUuid] = notification.entityUuid
            }.insertedCount > 0
        }.onFailure {
            Telemetry.error(tag = TAG, message = "Failed to save notification", throwable = it)
        }.getOrDefault(defaultValue = false) && instanceSignal.notify(channel = NOTIFICATION_CHANNEL, payload = notificationUserUuid.toString())
    }

    override suspend fun deleteNotification(notificationUuid: Uuid): Boolean = withContext(context = Dispatcher.IO) {
        database.safeTransaction {
            NotificationTable.deleteWhere { NotificationTable.uuid eq notificationUuid } > 0
        }.onFailure {
            Telemetry.error(tag = TAG, message = "Unable to delete pending notifications $notificationUuid", throwable = it)
        }.getOrDefault(defaultValue = false)
    }

    /**
     * Retrieves pending notifications for a user.
     *
     * @param userUuid The UUID of the user.
     * @return A list of pending notifications.
     */
    private suspend fun getNotifications(userUuid: Uuid): List<PushPayload> = database.safeTransaction {
        NotificationTable
            .selectAll()
            .where { NotificationTable.userUuid eq userUuid }
            .toList()
            .mapNotNull { it.toNotification() }
    }.onFailure {
        Telemetry.error(tag = TAG, message = "Unable to get pending notifications for user $userUuid", throwable = it)
    }.getOrDefault(defaultValue = emptyList())

    /**
     * Maps a database row to a [PushPayload.EntityNotification] domain model.
     *
     * @receiver A row from [NotificationTable].
     * @return The mapped [PushPayload.EntityNotification], or `null` if the stored entity type is unrecognized.
     */
    private fun ResultRow.toNotification(): PushPayload.EntityNotification? = PushPayload.EntityNotification(
        uuid = this[NotificationTable.uuid],
        userUuid = this[NotificationTable.userUuid],
        title = this[NotificationTable.title],
        description = this[NotificationTable.description],
        entityType = this[NotificationTable.entityType].toEnumOrNull() ?: run {
            Telemetry.error(tag = TAG, message = "Invalid entity type: ${this[NotificationTable.entityType]}")
            return null
        },
        entityUuid = this[NotificationTable.entityUuid],
    )

    companion object {
        private const val TAG = "NotificationGateway"

        /** The signal channel announcing the user UUID of every newly saved notification. */
        private const val NOTIFICATION_CHANNEL = "notification"
    }
}
