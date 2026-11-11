package com.app.builder.domain.usecase.equipment

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
import com.app.builder.data.database.table.EquipmentTable
import com.app.builder.data.database.table.UserAccountTable
import com.app.builder.data.serializer.decode
import com.app.builder.domain.EntityType
import com.app.builder.domain.Permission
import com.app.builder.domain.PushPayload
import com.app.builder.domain.Equipment
import com.app.builder.domain.usecase.Outcome
import com.app.builder.domain.usecase.notification.NotificationsUseCases
import com.app.builder.domain.usecase.toOutcome
import com.app.builder.toEnumOrNull

/**
 * Gateway implementation for equipments use cases.
 *
 * @property database The database instance used to execute transactions.
 * @property notificationsUseCases Used to save a notification for the users that can read equipments whenever an equipment is upserted.
 */
class EquipmentGateway(
    private val database: R2dbcDatabase,
    private val notificationsUseCases: NotificationsUseCases
): EquipmentUseCases {

    override suspend fun getEquipments(
        lastSyncUtc: Instant,
        cursorUtc: Instant?,
        cursorUuid: Uuid?,
        pageSize: Int
    ): Outcome<List<Equipment>> = withContext(context = Dispatcher.IO) {
        val effectivePageSize = pageSize.coerceIn(minimumValue = 1, maximumValue = ServerConfigs.value.maxPageSize)
        database.safeTransaction {
            EquipmentTable
                .selectAll()
                .where {
                    val sinceLastSync = EquipmentTable.modifiedAt greaterEq lastSyncUtc
                    when {
                        cursorUtc == null || cursorUuid == null -> sinceLastSync
                        else -> sinceLastSync and ((EquipmentTable.modifiedAt less cursorUtc) or ((EquipmentTable.modifiedAt eq cursorUtc) and (EquipmentTable.uuid less cursorUuid)))
                    }
                }
                .orderBy(EquipmentTable.modifiedAt to SortOrder.DESC, EquipmentTable.uuid to SortOrder.DESC)
                .limit(count = effectivePageSize)
                .toList()
                .mapNotNull { it.toEquipment() }
                .let { Outcome.Success(data = it) }
        }.getOrElse {
            Telemetry.error(tag = TAG, message = "Unable to get equipments since $lastSyncUtc", throwable = it)
            Outcome.Error
        }
    }

    override suspend fun getEquipment(uuid: Uuid): Outcome<Equipment> = withContext(context = Dispatcher.IO) {
        database.safeTransaction {
            EquipmentTable
                .selectAll()
                .where { EquipmentTable.uuid eq uuid }
                .firstOrNull()
                ?.toEquipment()
                ?.let { Outcome.Success(data = it) }
                ?: Outcome.NotFound
        }.getOrElse {
            Telemetry.error(tag = TAG, message = "Unable to get equipment $uuid", throwable = it)
            Outcome.Error
        }
    }

    override suspend fun upsertEquipment(equipment: Equipment): Outcome<Unit> = withContext(context = Dispatcher.IO) {
        val result = database.safeTransaction {
            EquipmentTable.upsert {
                it[EquipmentTable.uuid] = equipment.uuid
                it[EquipmentTable.modifiedAt] = equipment.modifiedAt
                it[EquipmentTable.deletedAt] = equipment.deletedAt
                it[EquipmentTable.name] = equipment.name
                it[EquipmentTable.description] = equipment.description
                it[EquipmentTable.status] = equipment.status.name
            }.insertedCount > 0
        }.onFailure {
            Telemetry.error(tag = TAG, message = "Unable to upsert equipment ${equipment.uuid}", throwable = it)
        }.getOrDefault(defaultValue = false).toOutcome()

        saveNotification(equipment = equipment)

        return@withContext result
    }

    /**
     * Persists a notification to the users with an active account that can read equipments.
     * If the notification service is active, it will trigger a push notification to the users.
     *
     * @param equipment The [equipment] to use for the notification.
     */
    private suspend fun saveNotification(equipment: Equipment) {
        val users = database.safeTransaction {
            UserAccountTable
                .select(UserAccountTable.userUuid, UserAccountTable.permissions)
                .where { UserAccountTable.deletedAt.isNull() }
                .toList()
                .filter { decode<Map<EntityType, Permission>>(value = it[UserAccountTable.permissions])?.get(EntityType.EQUIPMENT) != null }
                .map { it[UserAccountTable.userUuid] }
        }.onFailure {
            Telemetry.error(tag = TAG, message = "Unable to get users of this equipment", throwable = it)
        }.getOrDefault(defaultValue = emptyList())

        users.forEach { userUuid ->
            notificationsUseCases.saveNotification(
                PushPayload.EntityNotification(
                    uuid = uuid(),
                    userUuid = userUuid,
                    title = "equipment",
                    description = "equipment_updated",
                    entityType = EntityType.EQUIPMENT,
                    entityUuid = equipment.uuid,
                ),
            )
        }
    }

    /**
     * Maps a database row to an [Equipment] domain model.
     *
     * @receiver A row from the [EquipmentTable].
     * @return The mapped [Equipment], or `null` if the stored status is unrecognized.
     */
    private fun ResultRow.toEquipment(): Equipment? = Equipment(
        uuid = this[EquipmentTable.uuid],
        modifiedAt = this[EquipmentTable.modifiedAt],
        deletedAt = this[EquipmentTable.deletedAt],
        name = this[EquipmentTable.name],
        description = this[EquipmentTable.description],
        status = this[EquipmentTable.status].toEnumOrNull() ?: run {
            Telemetry.error(tag = TAG, message = "Invalid equipment status: ${this[EquipmentTable.status]}")
            return null
        }
    )

    companion object {
        private const val TAG = "EquipmentGateway"
    }
}
