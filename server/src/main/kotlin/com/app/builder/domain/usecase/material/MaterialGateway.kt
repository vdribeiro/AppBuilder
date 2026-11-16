package com.app.builder.domain.usecase.material

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
import com.app.builder.data.database.table.MaterialTable
import com.app.builder.data.database.table.UserAccountTable
import com.app.builder.data.serializer.decode
import com.app.builder.domain.EntityType
import com.app.builder.domain.Material
import com.app.builder.domain.Permission
import com.app.builder.domain.PushPayload
import com.app.builder.domain.usecase.Outcome
import com.app.builder.domain.usecase.notification.NotificationsUseCases
import com.app.builder.domain.usecase.toOutcome

/**
 * Gateway implementation for materials use cases.
 *
 * @property database The database instance used to execute transactions.
 * @property notificationsUseCases Used to save a notification for the users that can read materials whenever a material is upserted.
 */
class MaterialGateway(
    private val database: R2dbcDatabase,
    private val notificationsUseCases: NotificationsUseCases
): MaterialUseCases {

    override suspend fun getMaterials(
        lastSyncUtc: Instant,
        cursorUtc: Instant?,
        cursorUuid: Uuid?,
        pageSize: Int
    ): Outcome<List<Material>> = withContext(context = Dispatcher.IO) {
        val effectivePageSize = pageSize.coerceIn(minimumValue = 1, maximumValue = ServerConfigs.value.maxPageSize)
        database.safeTransaction {
            MaterialTable
                .selectAll()
                .where {
                    val sinceLastSync = MaterialTable.modifiedAt greaterEq lastSyncUtc
                    when {
                        cursorUtc == null || cursorUuid == null -> sinceLastSync
                        else -> sinceLastSync and ((MaterialTable.modifiedAt less cursorUtc) or ((MaterialTable.modifiedAt eq cursorUtc) and (MaterialTable.uuid less cursorUuid)))
                    }
                }
                .orderBy(MaterialTable.modifiedAt to SortOrder.DESC, MaterialTable.uuid to SortOrder.DESC)
                .limit(count = effectivePageSize)
                .toList()
                .map { it.toMaterial() }
                .let { Outcome.Success(data = it) }
        }.getOrElse {
            Telemetry.error(tag = TAG, message = "Unable to get materials since $lastSyncUtc", throwable = it)
            Outcome.Error
        }
    }

    override suspend fun getMaterial(uuid: Uuid): Outcome<Material> = withContext(context = Dispatcher.IO) {
        database.safeTransaction {
            MaterialTable
                .selectAll()
                .where { MaterialTable.uuid eq uuid }
                .firstOrNull()
                ?.toMaterial()
                ?.let { Outcome.Success(data = it) }
                ?: Outcome.NotFound
        }.getOrElse {
            Telemetry.error(tag = TAG, message = "Unable to get material $uuid", throwable = it)
            Outcome.Error
        }
    }

    override suspend fun upsertMaterial(material: Material): Outcome<Unit> = withContext(context = Dispatcher.IO) {
        val result = database.safeTransaction {
            MaterialTable.upsert {
                it[MaterialTable.uuid] = material.uuid
                it[MaterialTable.modifiedAt] = material.modifiedAt
                it[MaterialTable.deletedAt] = material.deletedAt
                it[MaterialTable.name] = material.name
                it[MaterialTable.description] = material.description
            }.insertedCount > 0
        }.onFailure {
            Telemetry.error(tag = TAG, message = "Unable to upsert material ${material.uuid}", throwable = it)
        }.getOrDefault(defaultValue = false).toOutcome()

        saveNotification(material = material)

        return@withContext result
    }

    /**
     * Persists a notification to the users with an active account that can read materials.
     * If the notification service is active, it will trigger a push notification to the users.
     *
     * @param material The [material] to use for the notification.
     */
    private suspend fun saveNotification(material: Material) {
        val users = database.safeTransaction {
            UserAccountTable
                .select(UserAccountTable.userUuid, UserAccountTable.permissions)
                .where { UserAccountTable.deletedAt.isNull() }
                .toList()
                .filter { decode<Map<EntityType, Permission>>(value = it[UserAccountTable.permissions])?.get(EntityType.MATERIAL) != null }
                .map { it[UserAccountTable.userUuid] }
        }.onFailure {
            Telemetry.error(tag = TAG, message = "Unable to get users of this material", throwable = it)
        }.getOrDefault(defaultValue = emptyList())

        users.forEach { userUuid ->
            notificationsUseCases.saveNotification(
                PushPayload.EntityNotification(
                    uuid = uuid(),
                    userUuid = userUuid,
                    title = "material",
                    description = "material_updated",
                    entityType = EntityType.MATERIAL,
                    entityUuid = material.uuid,
                ),
            )
        }
    }

    /**
     * Maps a database row to an [Material] domain model.
     *
     * @receiver A row from the [MaterialTable].
     * @return The mapped [Material].
     */
    private fun ResultRow.toMaterial(): Material = Material(
        uuid = this[MaterialTable.uuid],
        modifiedAt = this[MaterialTable.modifiedAt],
        deletedAt = this[MaterialTable.deletedAt],
        name = this[MaterialTable.name],
        description = this[MaterialTable.description]
    )

    companion object {
        private const val TAG = "MaterialGateway"
    }
}
