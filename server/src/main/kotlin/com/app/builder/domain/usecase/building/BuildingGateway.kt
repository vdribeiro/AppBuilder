package com.app.builder.domain.usecase.building

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
import com.app.builder.data.database.table.BuildingTable
import com.app.builder.data.database.table.UserAccountTable
import com.app.builder.data.serializer.decode
import com.app.builder.domain.EntityType
import com.app.builder.domain.Permission
import com.app.builder.domain.PushPayload
import com.app.builder.domain.Building
import com.app.builder.domain.usecase.Outcome
import com.app.builder.domain.usecase.notification.NotificationsUseCases
import com.app.builder.domain.usecase.toOutcome

/**
 * Gateway implementation for buildings use cases.
 *
 * @property database The database instance used to execute transactions.
 * @property notificationsUseCases Used to save a notification for the users that can read buildings whenever a building is upserted.
 */
class BuildingGateway(
    private val database: R2dbcDatabase,
    private val notificationsUseCases: NotificationsUseCases
): BuildingUseCases {

    override suspend fun getBuildings(
        lastSyncUtc: Instant,
        cursorUtc: Instant?,
        cursorUuid: Uuid?,
        pageSize: Int
    ): Outcome<List<Building>> = withContext(context = Dispatcher.IO) {
        val effectivePageSize = pageSize.coerceIn(minimumValue = 1, maximumValue = ServerConfigs.value.maxPageSize)
        database.safeTransaction {
            BuildingTable
                .selectAll()
                .where {
                    val sinceLastSync = BuildingTable.modifiedAt greaterEq lastSyncUtc
                    when {
                        cursorUtc == null || cursorUuid == null -> sinceLastSync
                        else -> sinceLastSync and ((BuildingTable.modifiedAt less cursorUtc) or ((BuildingTable.modifiedAt eq cursorUtc) and (BuildingTable.uuid less cursorUuid)))
                    }
                }
                .orderBy(BuildingTable.modifiedAt to SortOrder.DESC, BuildingTable.uuid to SortOrder.DESC)
                .limit(count = effectivePageSize)
                .toList()
                .map { it.toBuilding() }
                .let { Outcome.Success(data = it) }
        }.getOrElse {
            Telemetry.error(tag = TAG, message = "Unable to get buildings since $lastSyncUtc", throwable = it)
            Outcome.Error
        }
    }

    override suspend fun getBuilding(uuid: Uuid): Outcome<Building> = withContext(context = Dispatcher.IO) {
        database.safeTransaction {
            BuildingTable
                .selectAll()
                .where { BuildingTable.uuid eq uuid }
                .firstOrNull()
                ?.toBuilding()
                ?.let { Outcome.Success(data = it) }
                ?: Outcome.NotFound
        }.getOrElse {
            Telemetry.error(tag = TAG, message = "Unable to get building $uuid", throwable = it)
            Outcome.Error
        }
    }

    override suspend fun upsertBuilding(building: Building): Outcome<Unit> = withContext(context = Dispatcher.IO) {
        val result = database.safeTransaction {
            BuildingTable.upsert {
                it[BuildingTable.uuid] = building.uuid
                it[BuildingTable.modifiedAt] = building.modifiedAt
                it[BuildingTable.deletedAt] = building.deletedAt
                it[BuildingTable.name] = building.name
                it[BuildingTable.description] = building.description
                it[BuildingTable.address] = building.address
                it[BuildingTable.latitude] = building.latitude
                it[BuildingTable.longitude] = building.longitude
            }.insertedCount > 0
        }.onFailure {
            Telemetry.error(tag = TAG, message = "Unable to upsert building ${building.uuid}", throwable = it)
        }.getOrDefault(defaultValue = false).toOutcome()

        saveNotification(building = building)

        return@withContext result
    }

    /**
     * Persists a notification to the users with an active account that can read buildings.
     * If the notification service is active, it will trigger a push notification to the users.
     *
     * @param building The [building] to use for the notification.
     */
    private suspend fun saveNotification(building: Building) {
        val users = database.safeTransaction {
            UserAccountTable
                .select(UserAccountTable.userUuid, UserAccountTable.permissions)
                .where { UserAccountTable.deletedAt.isNull() }
                .toList()
                .filter { decode<Map<EntityType, Permission>>(value = it[UserAccountTable.permissions])?.get(EntityType.BUILDING) != null }
                .map { it[UserAccountTable.userUuid] }
        }.onFailure {
            Telemetry.error(tag = TAG, message = "Unable to get users of this building", throwable = it)
        }.getOrDefault(defaultValue = emptyList())

        users.forEach { userUuid ->
            notificationsUseCases.saveNotification(
                PushPayload.EntityNotification(
                    uuid = uuid(),
                    userUuid = userUuid,
                    title = "building",
                    description = "building_updated",
                    entityType = EntityType.BUILDING,
                    entityUuid = building.uuid,
                ),
            )
        }
    }

    /**
     * Maps a database row to a [Building] domain model.
     *
     * @receiver A row from the [BuildingTable].
     * @return The mapped [Building].
     */
    private fun ResultRow.toBuilding(): Building = Building(
        uuid = this[BuildingTable.uuid],
        modifiedAt = this[BuildingTable.modifiedAt],
        deletedAt = this[BuildingTable.deletedAt],
        name = this[BuildingTable.name],
        description = this[BuildingTable.description],
        address = this[BuildingTable.address],
        latitude = this[BuildingTable.latitude],
        longitude = this[BuildingTable.longitude]
    )

    companion object {
        private const val TAG = "BuildingGateway"
    }
}
