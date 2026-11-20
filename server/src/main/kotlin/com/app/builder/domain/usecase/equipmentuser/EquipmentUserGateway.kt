package com.app.builder.domain.usecase.equipmentuser

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
import com.app.builder.data.database.table.EquipmentUserTable
import com.app.builder.data.database.table.UserAccountTable
import com.app.builder.data.serializer.decode
import com.app.builder.domain.EntityType
import com.app.builder.domain.EquipmentUser
import com.app.builder.domain.Permission
import com.app.builder.domain.PushPayload
import com.app.builder.domain.grants
import com.app.builder.domain.usecase.Outcome
import com.app.builder.domain.usecase.notification.NotificationsUseCases
import com.app.builder.domain.usecase.toOutcome

/**
 * Gateway implementation for equipment users use cases.
 *
 * @property database The database instance used to execute transactions.
 * @property notificationsUseCases Used to save a notification for the users that can read equipment users whenever an equipment user is upserted.
 */
class EquipmentUserGateway(
    private val database: R2dbcDatabase,
    private val notificationsUseCases: NotificationsUseCases
): EquipmentUserUseCases {

    override suspend fun getEquipmentUsers(
        lastSyncUtc: Instant,
        cursorUtc: Instant?,
        cursorUuid: Uuid?,
        pageSize: Int
    ): Outcome<List<EquipmentUser>> = withContext(context = Dispatcher.IO) {
        val effectivePageSize = pageSize.coerceIn(minimumValue = 1, maximumValue = ServerConfigs.value.maxPageSize)
        database.safeTransaction {
            EquipmentUserTable
                .selectAll()
                .where {
                    val sinceLastSync = EquipmentUserTable.modifiedAt greaterEq lastSyncUtc
                    when {
                        cursorUtc == null || cursorUuid == null -> sinceLastSync
                        else -> sinceLastSync and ((EquipmentUserTable.modifiedAt less cursorUtc) or ((EquipmentUserTable.modifiedAt eq cursorUtc) and (EquipmentUserTable.uuid less cursorUuid)))
                    }
                }
                .orderBy(EquipmentUserTable.modifiedAt to SortOrder.DESC, EquipmentUserTable.uuid to SortOrder.DESC)
                .limit(count = effectivePageSize)
                .toList()
                .map { it.toEquipmentUser() }
                .let { Outcome.Success(data = it) }
        }.getOrElse {
            Telemetry.error(tag = TAG, message = "Unable to get equipment users since $lastSyncUtc", throwable = it)
            Outcome.Error
        }
    }

    override suspend fun getEquipmentUser(uuid: Uuid): Outcome<EquipmentUser> = withContext(context = Dispatcher.IO) {
        database.safeTransaction {
            EquipmentUserTable
                .selectAll()
                .where { EquipmentUserTable.uuid eq uuid }
                .firstOrNull()
                ?.toEquipmentUser()
                ?.let { Outcome.Success(data = it) }
                ?: Outcome.NotFound
        }.getOrElse {
            Telemetry.error(tag = TAG, message = "Unable to get equipment user $uuid", throwable = it)
            Outcome.Error
        }
    }

    override suspend fun upsertEquipmentUser(equipmentUser: EquipmentUser): Outcome<Unit> = withContext(context = Dispatcher.IO) {
        val result = database.safeTransaction {
            EquipmentUserTable.upsert {
                it[EquipmentUserTable.uuid] = equipmentUser.uuid
                it[EquipmentUserTable.modifiedAt] = equipmentUser.modifiedAt
                it[EquipmentUserTable.deletedAt] = equipmentUser.deletedAt
                it[EquipmentUserTable.equipmentUuid] = equipmentUser.equipmentUuid
                it[EquipmentUserTable.userUuid] = equipmentUser.userUuid
            }.insertedCount > 0
        }.onFailure {
            Telemetry.error(tag = TAG, message = "Unable to upsert equipment user ${equipmentUser.uuid}", throwable = it)
        }.getOrDefault(defaultValue = false).toOutcome()

        saveNotification(equipmentUser = equipmentUser)

        return@withContext result
    }

    /**
     * Persists a notification to the users with an active account that can read equipment users.
     * If the notification service is active, it will trigger a push notification to the users.
     *
     * @param equipmentUser The [EquipmentUser] to use for the notification.
     */
    private suspend fun saveNotification(equipmentUser: EquipmentUser) {
        val users = database.safeTransaction {
            UserAccountTable
                .select(UserAccountTable.userUuid, UserAccountTable.permissions)
                .where { UserAccountTable.deletedAt.isNull() }
                .toList()
                .filter { decode<Map<EntityType, Permission>>(value = it[UserAccountTable.permissions])?.grants(entityType = EntityType.EQUIPMENT_USER, permission = Permission.READ) == true }
                .map { it[UserAccountTable.userUuid] }
        }.onFailure {
            Telemetry.error(tag = TAG, message = "Unable to get users of this equipment user", throwable = it)
        }.getOrDefault(defaultValue = emptyList())

        users.forEach { userUuid ->
            notificationsUseCases.saveNotification(
                PushPayload.EntityNotification(
                    uuid = uuid(),
                    userUuid = userUuid,
                    title = "equipment_user",
                    description = "equipment_user_updated",
                    entityType = EntityType.EQUIPMENT_USER,
                    entityUuid = equipmentUser.uuid,
                ),
            )
        }
    }

    /**
     * Maps a database row to a [EquipmentUser] domain model.
     *
     * @receiver A row from the [EquipmentUserTable].
     * @return The mapped [EquipmentUser].
     */
    private fun ResultRow.toEquipmentUser(): EquipmentUser = EquipmentUser(
        modifiedAt = this[EquipmentUserTable.modifiedAt],
        deletedAt = this[EquipmentUserTable.deletedAt],
        equipmentUuid = this[EquipmentUserTable.equipmentUuid],
        userUuid = this[EquipmentUserTable.userUuid]
    )

    companion object {
        private const val TAG = "EquipmentUserGateway"
    }
}
