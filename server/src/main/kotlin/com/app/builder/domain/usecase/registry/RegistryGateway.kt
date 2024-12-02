package com.app.builder.domain.usecase.registry

import kotlin.time.Instant
import kotlin.uuid.Uuid
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.withContext
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.greaterEq
import org.jetbrains.exposed.v1.core.less
import org.jetbrains.exposed.v1.core.or
import org.jetbrains.exposed.v1.r2dbc.R2dbcDatabase
import org.jetbrains.exposed.v1.r2dbc.selectAll
import org.jetbrains.exposed.v1.r2dbc.upsert
import com.app.builder.core.config.ServerConfigs
import com.app.builder.core.flow.Dispatcher
import com.app.builder.core.platform.OS
import com.app.builder.core.telemetry.Telemetry
import com.app.builder.data.database.safeTransaction
import com.app.builder.data.database.table.RegistryTable
import com.app.builder.domain.EntityType
import com.app.builder.domain.Registry
import com.app.builder.toEnumOrNull

/**
 * Gateway implementation for the request registry use cases.
 *
 * @property database The database instance used to execute transactions.
 */
class RegistryGateway(
    private val database: R2dbcDatabase
): RegistryUseCases {

    override suspend fun getRegistries(
        lastSyncUtc: Instant,
        cursorUtc: Instant?,
        cursorUuid: Uuid?,
        pageSize: Int
    ): List<Registry> = withContext(context = Dispatcher.IO) {
        val effectivePageSize = pageSize.coerceIn(minimumValue = 1, maximumValue = ServerConfigs.configs.maxPageSize)
        database.safeTransaction {
            RegistryTable
                .selectAll()
                .where {
                    val sinceLastSync = RegistryTable.requestUtc greaterEq lastSyncUtc
                    when {
                        cursorUtc == null || cursorUuid == null -> sinceLastSync
                        else -> sinceLastSync and ((RegistryTable.requestUtc less cursorUtc) or ((RegistryTable.requestUtc eq cursorUtc) and (RegistryTable.requestUuid less cursorUuid)))
                    }
                }
                .orderBy(RegistryTable.requestUtc to SortOrder.DESC, RegistryTable.requestUuid to SortOrder.DESC)
                .limit(count = effectivePageSize)
                .toList()
                .map { it.toRegistry() }
        }.onFailure {
            Telemetry.error(tag = TAG, message = "Unable to get registries since $lastSyncUtc", throwable = it)
        }.getOrDefault(defaultValue = emptyList())
    }

    override suspend fun upsertRegistry(registry: Registry): Boolean = withContext(context = Dispatcher.IO) {
        database.safeTransaction {
            RegistryTable.upsert {
                it[requestUuid] = registry.requestUuid
                it[requestType] = registry.requestType
                it[requestUtc] = registry.requestUtc
                it[requestSentUtc] = registry.requestSentUtc
                it[userUuid] = registry.userUuid
                it[appVersion] = registry.appVersion
                it[os] = registry.os.toString()
                it[osVersion] = registry.osVersion
                it[brand] = registry.brand
                it[model] = registry.model
                it[deviceUuid] = registry.deviceUuid
                it[entityUuid] = registry.entityUuid
                it[entityType] = registry.entityType?.name
                it[payload] = registry.payload
            }.insertedCount > 0
        }.onFailure {
            Telemetry.error(tag = TAG, message = "Unable to save registry ${registry.requestUuid}", throwable = it)
        }.getOrDefault(defaultValue = false)
    }

    /**
     * Maps a database row to a [Registry] domain model.
     *
     * @receiver A row from [RegistryTable].
     * @return The mapped [Registry].
     */
    private fun ResultRow.toRegistry(): Registry = Registry(
        requestUuid = this[RegistryTable.requestUuid],
        requestType = this[RegistryTable.requestType],
        requestUtc = this[RegistryTable.requestUtc],
        requestSentUtc = this[RegistryTable.requestSentUtc],
        deletedAt = this[RegistryTable.deletedAt],
        userUuid = this[RegistryTable.userUuid],
        appVersion = this[RegistryTable.appVersion],
        os = OS.fromString(value = this[RegistryTable.os]),
        osVersion = this[RegistryTable.osVersion],
        brand = this[RegistryTable.brand],
        model = this[RegistryTable.model],
        deviceUuid = this[RegistryTable.deviceUuid],
        entityUuid = this[RegistryTable.entityUuid],
        entityType = this[RegistryTable.entityType]?.toEnumOrNull(),
        payload = this[RegistryTable.payload],
    )

    companion object {
        private const val TAG = "RegistryGateway"
    }
}
