package com.app.builder.domain.usecase.buildingmaterial

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
import com.app.builder.data.database.table.BuildingMaterialTable
import com.app.builder.data.database.table.UserAccountTable
import com.app.builder.data.serializer.decode
import com.app.builder.domain.BuildingMaterial
import com.app.builder.domain.EntityType
import com.app.builder.domain.Permission
import com.app.builder.domain.PushPayload
import com.app.builder.domain.grants
import com.app.builder.domain.usecase.Outcome
import com.app.builder.domain.usecase.notification.NotificationsUseCases
import com.app.builder.domain.usecase.toOutcome

/**
 * Gateway implementation for building materials use cases.
 *
 * @property database The database instance used to execute transactions.
 * @property notificationsUseCases Used to save a notification for the users that can read building materials whenever a building material is upserted.
 */
class BuildingMaterialGateway(
    private val database: R2dbcDatabase,
    private val notificationsUseCases: NotificationsUseCases
): BuildingMaterialUseCases {

    override suspend fun getBuildingMaterials(
        lastSyncUtc: Instant,
        cursorUtc: Instant?,
        cursorUuid: Uuid?,
        pageSize: Int
    ): Outcome<List<BuildingMaterial>> = withContext(context = Dispatcher.IO) {
        val effectivePageSize = pageSize.coerceIn(minimumValue = 1, maximumValue = ServerConfigs.value.maxPageSize)
        database.safeTransaction {
            BuildingMaterialTable
                .selectAll()
                .where {
                    val sinceLastSync = BuildingMaterialTable.modifiedAt greaterEq lastSyncUtc
                    when {
                        cursorUtc == null || cursorUuid == null -> sinceLastSync
                        else -> sinceLastSync and ((BuildingMaterialTable.modifiedAt less cursorUtc) or ((BuildingMaterialTable.modifiedAt eq cursorUtc) and (BuildingMaterialTable.uuid less cursorUuid)))
                    }
                }
                .orderBy(BuildingMaterialTable.modifiedAt to SortOrder.DESC, BuildingMaterialTable.uuid to SortOrder.DESC)
                .limit(count = effectivePageSize)
                .toList()
                .map { it.toBuildingMaterial() }
                .let { Outcome.Success(data = it) }
        }.getOrElse {
            Telemetry.error(tag = TAG, message = "Unable to get building materials since $lastSyncUtc", throwable = it)
            Outcome.Error
        }
    }

    override suspend fun getBuildingMaterial(uuid: Uuid): Outcome<BuildingMaterial> = withContext(context = Dispatcher.IO) {
        database.safeTransaction {
            BuildingMaterialTable
                .selectAll()
                .where { BuildingMaterialTable.uuid eq uuid }
                .firstOrNull()
                ?.toBuildingMaterial()
                ?.let { Outcome.Success(data = it) }
                ?: Outcome.NotFound
        }.getOrElse {
            Telemetry.error(tag = TAG, message = "Unable to get building material $uuid", throwable = it)
            Outcome.Error
        }
    }

    override suspend fun upsertBuildingMaterial(buildingMaterial: BuildingMaterial): Outcome<Unit> = withContext(context = Dispatcher.IO) {
        val result = database.safeTransaction {
            BuildingMaterialTable.upsert {
                it[BuildingMaterialTable.uuid] = buildingMaterial.uuid
                it[BuildingMaterialTable.modifiedAt] = buildingMaterial.modifiedAt
                it[BuildingMaterialTable.deletedAt] = buildingMaterial.deletedAt
                it[BuildingMaterialTable.buildingUuid] = buildingMaterial.buildingUuid
                it[BuildingMaterialTable.materialUuid] = buildingMaterial.materialUuid
                it[BuildingMaterialTable.quantity] = buildingMaterial.quantity
            }.insertedCount > 0
        }.onFailure {
            Telemetry.error(tag = TAG, message = "Unable to upsert building material ${buildingMaterial.uuid}", throwable = it)
        }.getOrDefault(defaultValue = false).toOutcome()

        saveNotification(buildingMaterial = buildingMaterial)

        return@withContext result
    }

    /**
     * Persists a notification to the users with an active account that can read building materials.
     * If the notification service is active, it will trigger a push notification to the users.
     *
     * @param buildingMaterial The [BuildingMaterial] to use for the notification.
     */
    private suspend fun saveNotification(buildingMaterial: BuildingMaterial) {
        val users = database.safeTransaction {
            UserAccountTable
                .select(UserAccountTable.userUuid, UserAccountTable.permissions)
                .where { UserAccountTable.deletedAt.isNull() }
                .toList()
                .filter { decode<Map<EntityType, Permission>>(value = it[UserAccountTable.permissions])?.grants(entityType = EntityType.BUILDING_MATERIAL, permission = Permission.READ) == true }
                .map { it[UserAccountTable.userUuid] }
        }.onFailure {
            Telemetry.error(tag = TAG, message = "Unable to get users of this building material", throwable = it)
        }.getOrDefault(defaultValue = emptyList())

        users.forEach { userUuid ->
            notificationsUseCases.saveNotification(
                PushPayload.EntityNotification(
                    uuid = uuid(),
                    userUuid = userUuid,
                    title = "building_material",
                    description = "building_material_updated",
                    entityType = EntityType.BUILDING_MATERIAL,
                    entityUuid = buildingMaterial.uuid,
                ),
            )
        }
    }

    /**
     * Maps a database row to a [BuildingMaterial] domain model.
     *
     * @receiver A row from the [BuildingMaterialTable].
     * @return The mapped [BuildingMaterial].
     */
    private fun ResultRow.toBuildingMaterial(): BuildingMaterial = BuildingMaterial(
        modifiedAt = this[BuildingMaterialTable.modifiedAt],
        deletedAt = this[BuildingMaterialTable.deletedAt],
        buildingUuid = this[BuildingMaterialTable.buildingUuid],
        materialUuid = this[BuildingMaterialTable.materialUuid],
        quantity = this[BuildingMaterialTable.quantity]
    )

    companion object {
        private const val TAG = "BuildingMaterialGateway"
    }
}
