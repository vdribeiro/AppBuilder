package com.app.builder.domain.usecase.buildingequipment

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
import com.app.builder.data.database.table.BuildingEquipmentTable
import com.app.builder.data.database.table.UserAccountTable
import com.app.builder.data.serializer.decode
import com.app.builder.domain.EntityType
import com.app.builder.domain.BuildingEquipment
import com.app.builder.domain.Permission
import com.app.builder.domain.PushPayload
import com.app.builder.domain.grants
import com.app.builder.domain.usecase.Outcome
import com.app.builder.domain.usecase.notification.NotificationsUseCases
import com.app.builder.domain.usecase.toOutcome

/**
 * Gateway implementation for building equipments use cases.
 *
 * @property database The database instance used to execute transactions.
 * @property notificationsUseCases Used to save a notification for the users that can read building equipments whenever a building equipment is upserted.
 */
class BuildingEquipmentGateway(
    private val database: R2dbcDatabase,
    private val notificationsUseCases: NotificationsUseCases
): BuildingEquipmentUseCases {

    override suspend fun getBuildingEquipments(
        lastSyncUtc: Instant,
        cursorUtc: Instant?,
        cursorUuid: Uuid?,
        pageSize: Int
    ): Outcome<List<BuildingEquipment>> = withContext(context = Dispatcher.IO) {
        val effectivePageSize = pageSize.coerceIn(minimumValue = 1, maximumValue = ServerConfigs.value.maxPageSize)
        database.safeTransaction {
            BuildingEquipmentTable
                .selectAll()
                .where {
                    val sinceLastSync = BuildingEquipmentTable.modifiedAt greaterEq lastSyncUtc
                    when {
                        cursorUtc == null || cursorUuid == null -> sinceLastSync
                        else -> sinceLastSync and ((BuildingEquipmentTable.modifiedAt less cursorUtc) or ((BuildingEquipmentTable.modifiedAt eq cursorUtc) and (BuildingEquipmentTable.uuid less cursorUuid)))
                    }
                }
                .orderBy(BuildingEquipmentTable.modifiedAt to SortOrder.DESC, BuildingEquipmentTable.uuid to SortOrder.DESC)
                .limit(count = effectivePageSize)
                .toList()
                .map { it.toBuildingEquipment() }
                .let { Outcome.Success(data = it) }
        }.getOrElse {
            Telemetry.error(tag = TAG, message = "Unable to get building equipments since $lastSyncUtc", throwable = it)
            Outcome.Error
        }
    }

    override suspend fun getBuildingEquipment(uuid: Uuid): Outcome<BuildingEquipment> = withContext(context = Dispatcher.IO) {
        database.safeTransaction {
            BuildingEquipmentTable
                .selectAll()
                .where { BuildingEquipmentTable.uuid eq uuid }
                .firstOrNull()
                ?.toBuildingEquipment()
                ?.let { Outcome.Success(data = it) }
                ?: Outcome.NotFound
        }.getOrElse {
            Telemetry.error(tag = TAG, message = "Unable to get building equipment $uuid", throwable = it)
            Outcome.Error
        }
    }

    override suspend fun upsertBuildingEquipment(buildingEquipment: BuildingEquipment): Outcome<Unit> = withContext(context = Dispatcher.IO) {
        val result = database.safeTransaction {
            BuildingEquipmentTable.upsert {
                it[BuildingEquipmentTable.uuid] = buildingEquipment.uuid
                it[BuildingEquipmentTable.modifiedAt] = buildingEquipment.modifiedAt
                it[BuildingEquipmentTable.deletedAt] = buildingEquipment.deletedAt
                it[BuildingEquipmentTable.buildingUuid] = buildingEquipment.buildingUuid
                it[BuildingEquipmentTable.equipmentUuid] = buildingEquipment.equipmentUuid
            }.insertedCount > 0
        }.onFailure {
            Telemetry.error(tag = TAG, message = "Unable to upsert building equipment ${buildingEquipment.uuid}", throwable = it)
        }.getOrDefault(defaultValue = false).toOutcome()

        saveNotification(buildingEquipment = buildingEquipment)

        return@withContext result
    }

    /**
     * Persists a notification to the users with an active account that can read building equipments.
     * If the notification service is active, it will trigger a push notification to the users.
     *
     * @param buildingEquipment The [BuildingEquipment] to use for the notification.
     */
    private suspend fun saveNotification(buildingEquipment: BuildingEquipment) {
        val users = database.safeTransaction {
            UserAccountTable
                .select(UserAccountTable.userUuid, UserAccountTable.permissions)
                .where { UserAccountTable.deletedAt.isNull() }
                .toList()
                .filter { decode<Map<EntityType, Permission>>(value = it[UserAccountTable.permissions])?.grants(entityType = EntityType.BUILDING_EQUIPMENT, permission = Permission.READ) == true }
                .map { it[UserAccountTable.userUuid] }
        }.onFailure {
            Telemetry.error(tag = TAG, message = "Unable to get users of this building equipment", throwable = it)
        }.getOrDefault(defaultValue = emptyList())

        users.forEach { userUuid ->
            notificationsUseCases.saveNotification(
                PushPayload.EntityNotification(
                    uuid = uuid(),
                    userUuid = userUuid,
                    title = "building_equipment",
                    description = "building_equipment_updated",
                    entityType = EntityType.BUILDING_EQUIPMENT,
                    entityUuid = buildingEquipment.uuid,
                ),
            )
        }
    }

    /**
     * Maps a database row to a [BuildingEquipment] domain model.
     *
     * @receiver A row from the [BuildingEquipmentTable].
     * @return The mapped [BuildingEquipment].
     */
    private fun ResultRow.toBuildingEquipment(): BuildingEquipment = BuildingEquipment(
        modifiedAt = this[BuildingEquipmentTable.modifiedAt],
        deletedAt = this[BuildingEquipmentTable.deletedAt],
        buildingUuid = this[BuildingEquipmentTable.buildingUuid],
        equipmentUuid = this[BuildingEquipmentTable.equipmentUuid]
    )

    companion object {
        private const val TAG = "BuildingEquipmentGateway"
    }
}
