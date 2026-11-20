package com.app.builder.domain.usecase.buildinguser

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
import com.app.builder.data.database.table.BuildingUserTable
import com.app.builder.data.database.table.UserAccountTable
import com.app.builder.data.serializer.decode
import com.app.builder.domain.BuildingUser
import com.app.builder.domain.EntityType
import com.app.builder.domain.Permission
import com.app.builder.domain.PushPayload
import com.app.builder.domain.grants
import com.app.builder.domain.usecase.Outcome
import com.app.builder.domain.usecase.notification.NotificationsUseCases
import com.app.builder.domain.usecase.toOutcome

/**
 * Gateway implementation for building users use cases.
 *
 * @property database The database instance used to execute transactions.
 * @property notificationsUseCases Used to save a notification for the users that can read building users whenever a building user is upserted.
 */
class BuildingUserGateway(
    private val database: R2dbcDatabase,
    private val notificationsUseCases: NotificationsUseCases
): BuildingUserUseCases {

    override suspend fun getBuildingUsers(
        lastSyncUtc: Instant,
        cursorUtc: Instant?,
        cursorUuid: Uuid?,
        pageSize: Int
    ): Outcome<List<BuildingUser>> = withContext(context = Dispatcher.IO) {
        val effectivePageSize = pageSize.coerceIn(minimumValue = 1, maximumValue = ServerConfigs.value.maxPageSize)
        database.safeTransaction {
            BuildingUserTable
                .selectAll()
                .where {
                    val sinceLastSync = BuildingUserTable.modifiedAt greaterEq lastSyncUtc
                    when {
                        cursorUtc == null || cursorUuid == null -> sinceLastSync
                        else -> sinceLastSync and ((BuildingUserTable.modifiedAt less cursorUtc) or ((BuildingUserTable.modifiedAt eq cursorUtc) and (BuildingUserTable.uuid less cursorUuid)))
                    }
                }
                .orderBy(BuildingUserTable.modifiedAt to SortOrder.DESC, BuildingUserTable.uuid to SortOrder.DESC)
                .limit(count = effectivePageSize)
                .toList()
                .map { it.toBuildingUser() }
                .let { Outcome.Success(data = it) }
        }.getOrElse {
            Telemetry.error(tag = TAG, message = "Unable to get building users since $lastSyncUtc", throwable = it)
            Outcome.Error
        }
    }

    override suspend fun getBuildingUser(uuid: Uuid): Outcome<BuildingUser> = withContext(context = Dispatcher.IO) {
        database.safeTransaction {
            BuildingUserTable
                .selectAll()
                .where { BuildingUserTable.uuid eq uuid }
                .firstOrNull()
                ?.toBuildingUser()
                ?.let { Outcome.Success(data = it) }
                ?: Outcome.NotFound
        }.getOrElse {
            Telemetry.error(tag = TAG, message = "Unable to get building user $uuid", throwable = it)
            Outcome.Error
        }
    }

    override suspend fun upsertBuildingUser(buildingUser: BuildingUser): Outcome<Unit> = withContext(context = Dispatcher.IO) {
        val result = database.safeTransaction {
            BuildingUserTable.upsert {
                it[BuildingUserTable.uuid] = buildingUser.uuid
                it[BuildingUserTable.modifiedAt] = buildingUser.modifiedAt
                it[BuildingUserTable.deletedAt] = buildingUser.deletedAt
                it[BuildingUserTable.buildingUuid] = buildingUser.buildingUuid
                it[BuildingUserTable.userUuid] = buildingUser.userUuid
            }.insertedCount > 0
        }.onFailure {
            Telemetry.error(tag = TAG, message = "Unable to upsert building user ${buildingUser.uuid}", throwable = it)
        }.getOrDefault(defaultValue = false).toOutcome()

        saveNotification(buildingUser = buildingUser)

        return@withContext result
    }

    /**
     * Persists a notification to the users with an active account that can read building users.
     * If the notification service is active, it will trigger a push notification to the users.
     *
     * @param buildingUser The [BuildingUser] to use for the notification.
     */
    private suspend fun saveNotification(buildingUser: BuildingUser) {
        val users = database.safeTransaction {
            UserAccountTable
                .select(UserAccountTable.userUuid, UserAccountTable.permissions)
                .where { UserAccountTable.deletedAt.isNull() }
                .toList()
                .filter { decode<Map<EntityType, Permission>>(value = it[UserAccountTable.permissions])?.grants(entityType = EntityType.BUILDING_USER, permission = Permission.READ) == true }
                .map { it[UserAccountTable.userUuid] }
        }.onFailure {
            Telemetry.error(tag = TAG, message = "Unable to get users of this building user", throwable = it)
        }.getOrDefault(defaultValue = emptyList())

        users.forEach { userUuid ->
            notificationsUseCases.saveNotification(
                PushPayload.EntityNotification(
                    uuid = uuid(),
                    userUuid = userUuid,
                    title = "building_user",
                    description = "building_user_updated",
                    entityType = EntityType.BUILDING_USER,
                    entityUuid = buildingUser.uuid,
                ),
            )
        }
    }

    /**
     * Maps a database row to a [BuildingUser] domain model.
     *
     * @receiver A row from the [BuildingUserTable].
     * @return The mapped [BuildingUser].
     */
    private fun ResultRow.toBuildingUser(): BuildingUser = BuildingUser(
        modifiedAt = this[BuildingUserTable.modifiedAt],
        deletedAt = this[BuildingUserTable.deletedAt],
        buildingUuid = this[BuildingUserTable.buildingUuid],
        userUuid = this[BuildingUserTable.userUuid]
    )

    companion object {
        private const val TAG = "BuildingUserGateway"
    }
}
