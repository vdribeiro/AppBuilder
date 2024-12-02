package com.app.builder.domain.usecase.connection

import kotlin.time.Duration.Companion.milliseconds
import kotlin.uuid.Uuid
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.withContext
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.greaterEq
import org.jetbrains.exposed.v1.core.less
import org.jetbrains.exposed.v1.core.neq
import org.jetbrains.exposed.v1.r2dbc.R2dbcDatabase
import org.jetbrains.exposed.v1.r2dbc.deleteWhere
import org.jetbrains.exposed.v1.r2dbc.selectAll
import org.jetbrains.exposed.v1.r2dbc.update
import org.jetbrains.exposed.v1.r2dbc.upsert
import com.app.builder.core.config.ServerConfigs
import com.app.builder.core.flow.Dispatcher
import com.app.builder.core.locale.now
import com.app.builder.core.telemetry.Telemetry
import com.app.builder.data.database.safeTransaction
import com.app.builder.data.database.table.ConnectionTable

/**
 * Gateway implementation for connection presence use cases.
 *
 * @property database The database instance used to execute transactions.
 */
class ConnectionGateway(
    private val database: R2dbcDatabase
): ConnectionUseCases {

    override suspend fun addConnection(userUuid: Uuid, deviceUuid: Uuid, instanceId: Uuid): Boolean = withContext(context = Dispatcher.IO) {
        val now = now()
        val liveSince = now - ServerConfigs.configs.presenceTtl.milliseconds
        val pushMaxConnections = ServerConfigs.configs.pushMaxConnections
        val pushMaxUserConnections = ServerConfigs.configs.pushMaxUserConnections

        database.safeTransaction {
            val liveConnections = ConnectionTable
                .selectAll()
                .where { (ConnectionTable.updatedAt greaterEq liveSince) and (ConnectionTable.deviceUuid neq deviceUuid) }
                .map { it[ConnectionTable.userUuid] }
                .toList()

            if (liveConnections.size >= pushMaxConnections) {
                Telemetry.error(tag = TAG, message = "Connection rejected: global limit reached (${liveConnections.size}/${pushMaxConnections})")
                return@safeTransaction false
            }
            if (liveConnections.count { it == userUuid } >= pushMaxUserConnections) {
                Telemetry.error(tag = TAG, message = "Connection rejected: per-user limit reached for user $userUuid")
                return@safeTransaction false
            }

            ConnectionTable.upsert {
                it[ConnectionTable.deviceUuid] = deviceUuid
                it[ConnectionTable.userUuid] = userUuid
                it[ConnectionTable.instanceId] = instanceId
                it[ConnectionTable.updatedAt] = now
            }.insertedCount > 0
        }.onFailure {
            Telemetry.error(tag = TAG, message = "Unable to add connection for device $deviceUuid", throwable = it)
        }.getOrDefault(defaultValue = false)
    }

    override suspend fun removeConnection(deviceUuid: Uuid, instanceId: Uuid): Boolean = withContext(context = Dispatcher.IO) {
        database.safeTransaction {
            ConnectionTable.deleteWhere { (ConnectionTable.deviceUuid eq deviceUuid) and (ConnectionTable.instanceId eq instanceId) } > 0
        }.onFailure {
            Telemetry.error(tag = TAG, message = "Unable to remove connection for device $deviceUuid", throwable = it)
        }.getOrDefault(defaultValue = false)
    }

    override suspend fun getConnectedDeviceUuids(userUuid: Uuid): List<Uuid> = withContext(context = Dispatcher.IO) {
        val liveSince = now() - ServerConfigs.configs.presenceTtl.milliseconds

        database.safeTransaction {
            ConnectionTable
                .selectAll()
                .where { (ConnectionTable.userUuid eq userUuid) and (ConnectionTable.updatedAt greaterEq liveSince) }
                .map { it[ConnectionTable.deviceUuid] }
                .toList()
        }.onFailure {
            Telemetry.error(tag = TAG, message = "Unable to get connected devices for user $userUuid", throwable = it)
        }.getOrDefault(defaultValue = emptyList())
    }

    override suspend fun touchConnections(instanceId: Uuid): Unit = withContext(context = Dispatcher.IO) {
        val now = now()
        val liveSince = now - ServerConfigs.configs.presenceTtl.milliseconds

        database.safeTransaction {
            ConnectionTable.update(where = { ConnectionTable.instanceId eq instanceId }) {
                it[ConnectionTable.updatedAt] = now
            }
            ConnectionTable.deleteWhere { ConnectionTable.updatedAt less liveSince }
        }.onFailure {
            Telemetry.error(tag = TAG, message = "Unable to refresh connection presence for instance $instanceId", throwable = it)
        }
    }

    companion object {
        private const val TAG = "ConnectionGateway"
    }
}
