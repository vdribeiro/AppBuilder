package com.app.builder.domain.usecase.timepunch

import kotlin.time.Instant
import kotlin.uuid.Uuid
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.withContext
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.greaterEq
import org.jetbrains.exposed.v1.core.isNull
import org.jetbrains.exposed.v1.core.less
import org.jetbrains.exposed.v1.core.or
import org.jetbrains.exposed.v1.r2dbc.R2dbcDatabase
import org.jetbrains.exposed.v1.r2dbc.select
import org.jetbrains.exposed.v1.r2dbc.selectAll
import org.jetbrains.exposed.v1.r2dbc.upsert
import com.app.builder.core.config.ServerConfigs
import com.app.builder.core.flow.Dispatcher
import com.app.builder.core.security.uuid
import com.app.builder.core.telemetry.Telemetry
import com.app.builder.data.database.safeTransaction
import com.app.builder.data.database.table.TimePunchTable
import com.app.builder.data.database.table.UserAccountTable
import com.app.builder.data.serializer.decode
import com.app.builder.domain.EntityType
import com.app.builder.domain.Permission
import com.app.builder.domain.PushPayload
import com.app.builder.domain.TimePunch
import com.app.builder.domain.usecase.Outcome
import com.app.builder.domain.usecase.notification.NotificationsUseCases
import com.app.builder.domain.usecase.toOutcome
import com.app.builder.toEnumOrNull

/**
 * Gateway implementation for time punches use cases.
 *
 * @property database The database instance used to execute transactions.
 * @property notificationsUseCases Used to save a notification for the users that can read a time punch whenever it is upserted.
 */
class TimePunchGateway(
    private val database: R2dbcDatabase,
    private val notificationsUseCases: NotificationsUseCases
): TimePunchUseCases {

    override suspend fun getTimePunches(
        userUuid: Uuid?,
        lastSyncUtc: Instant,
        cursorUtc: Instant?,
        cursorUuid: Uuid?,
        pageSize: Int
    ): Outcome<List<TimePunch>> = withContext(context = Dispatcher.IO) {
        val effectivePageSize = pageSize.coerceIn(minimumValue = 1, maximumValue = ServerConfigs.value.maxPageSize)
        database.safeTransaction {
            TimePunchTable
                .selectAll()
                .where {
                    val sinceLastSync = TimePunchTable.modifiedAt greaterEq lastSyncUtc
                    val page = when {
                        cursorUtc == null || cursorUuid == null -> sinceLastSync
                        else -> sinceLastSync and ((TimePunchTable.modifiedAt less cursorUtc) or ((TimePunchTable.modifiedAt eq cursorUtc) and (TimePunchTable.uuid less cursorUuid)))
                    }
                    if (userUuid == null) page else page and (TimePunchTable.userUuid eq userUuid)
                }
                .orderBy(TimePunchTable.modifiedAt to SortOrder.DESC, TimePunchTable.uuid to SortOrder.DESC)
                .limit(count = effectivePageSize)
                .toList()
                .mapNotNull { it.toTimePunch() }
                .let { Outcome.Success(data = it) }
        }.getOrElse {
            Telemetry.error(tag = TAG, message = "Unable to get time punches since $lastSyncUtc", throwable = it)
            Outcome.Error
        }
    }

    override suspend fun getTimePunch(uuid: Uuid): Outcome<TimePunch> = withContext(context = Dispatcher.IO) {
        database.safeTransaction {
            TimePunchTable
                .selectAll()
                .where { TimePunchTable.uuid eq uuid }
                .firstOrNull()
                ?.toTimePunch()
                ?.let { Outcome.Success(data = it) }
                ?: Outcome.NotFound
        }.getOrElse {
            Telemetry.error(tag = TAG, message = "Unable to get time punch $uuid", throwable = it)
            Outcome.Error
        }
    }

    override suspend fun upsertTimePunch(timePunch: TimePunch): Outcome<Unit> = withContext(context = Dispatcher.IO) {
        val result = database.safeTransaction {
            TimePunchTable.upsert {
                it[TimePunchTable.uuid] = timePunch.uuid
                it[TimePunchTable.modifiedAt] = timePunch.modifiedAt
                it[TimePunchTable.deletedAt] = timePunch.deletedAt
                it[TimePunchTable.userUuid] = timePunch.userUuid
                it[TimePunchTable.time] = timePunch.time
                it[TimePunchTable.punchSource] = timePunch.source.name
            }.insertedCount > 0
        }.onFailure {
            Telemetry.error(tag = TAG, message = "Unable to upsert time punch ${timePunch.uuid}", throwable = it)
        }.getOrDefault(defaultValue = false).toOutcome()

        saveNotification(timePunch = timePunch)

        return@withContext result
    }

    /**
     * Persists a notification to the users with an active account that can read the time punch: its user, and the users with a time punch permission.
     * If the notification service is active, it will trigger a push notification to the users.
     *
     * @param timePunch The [timePunch] to use for the notification.
     */
    private suspend fun saveNotification(timePunch: TimePunch) {
        val users = database.safeTransaction {
            UserAccountTable
                .select(UserAccountTable.userUuid, UserAccountTable.permissions)
                .where { UserAccountTable.deletedAt.isNull() }
                .toList()
                .filter { it[UserAccountTable.userUuid] == timePunch.userUuid || decode<Map<EntityType, Permission>>(value = it[UserAccountTable.permissions])?.get(EntityType.TIME_PUNCH) != null }
                .map { it[UserAccountTable.userUuid] }
        }.onFailure {
            Telemetry.error(tag = TAG, message = "Unable to get users of this time punch", throwable = it)
        }.getOrDefault(defaultValue = emptyList())

        users.forEach { userUuid ->
            notificationsUseCases.saveNotification(
                PushPayload.EntityNotification(
                    uuid = uuid(),
                    userUuid = userUuid,
                    title = "time_punch",
                    description = "time_punch_updated",
                    entityType = EntityType.TIME_PUNCH,
                    entityUuid = timePunch.uuid,
                ),
            )
        }
    }

    /**
     * Maps a database row to a [TimePunch] domain model.
     *
     * @receiver A row from the [TimePunchTable].
     * @return The mapped [TimePunch], or `null` if the stored source is unrecognized.
     */
    private fun ResultRow.toTimePunch(): TimePunch? = TimePunch(
        uuid = this[TimePunchTable.uuid],
        modifiedAt = this[TimePunchTable.modifiedAt],
        deletedAt = this[TimePunchTable.deletedAt],
        userUuid = this[TimePunchTable.userUuid],
        time = this[TimePunchTable.time],
        source = this[TimePunchTable.punchSource].toEnumOrNull() ?: run {
            Telemetry.error(tag = TAG, message = "Invalid time punch source: ${this[TimePunchTable.punchSource]}")
            return null
        }
    )

    companion object {
        private const val TAG = "TimePunchGateway"
    }
}
