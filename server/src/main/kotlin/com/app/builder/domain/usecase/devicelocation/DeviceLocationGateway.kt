package com.app.builder.domain.usecase.devicelocation

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
import com.app.builder.core.telemetry.Telemetry
import com.app.builder.data.database.safeTransaction
import com.app.builder.data.database.table.DeviceLocationTable
import com.app.builder.domain.DeviceLocation

/**
 * Gateway implementation for device location use cases.
 *
 * @property database The database instance used to execute transactions.
 */
class DeviceLocationGateway(
    private val database: R2dbcDatabase
): DeviceLocationUseCases {

    override suspend fun getDeviceLocations(
        userUuid: Uuid,
        lastSyncUtc: Instant,
        cursorUtc: Instant?,
        cursorUuid: Uuid?,
        pageSize: Int
    ): List<DeviceLocation> = withContext(context = Dispatcher.IO) {
        val effectivePageSize = pageSize.coerceIn(minimumValue = 1, maximumValue = ServerConfigs.configs.maxPageSize)
        database.safeTransaction {
            DeviceLocationTable
                .selectAll()
                .where {
                    val sinceLastSync = (DeviceLocationTable.userUuid eq userUuid) and (DeviceLocationTable.deviceTime greaterEq lastSyncUtc)
                    when {
                        cursorUtc == null || cursorUuid == null -> sinceLastSync
                        else -> sinceLastSync and ((DeviceLocationTable.deviceTime less cursorUtc) or ((DeviceLocationTable.deviceTime eq cursorUtc) and (DeviceLocationTable.uuid less cursorUuid)))
                    }
                }
                .orderBy(DeviceLocationTable.deviceTime to SortOrder.DESC, DeviceLocationTable.uuid to SortOrder.DESC)
                .limit(count = effectivePageSize)
                .toList()
                .map { it.toDeviceLocation() }
        }.onFailure {
            Telemetry.error(tag = TAG, message = "Unable to get registries since $lastSyncUtc", throwable = it)
        }.getOrDefault(defaultValue = emptyList())
    }

    override suspend fun upsertDeviceLocation(userUuid: Uuid, deviceLocation: DeviceLocation): Boolean = withContext(context = Dispatcher.IO) {
        database.safeTransaction {
            DeviceLocationTable.upsert {
                it[DeviceLocationTable.uuid] = deviceLocation.uuid
                it[DeviceLocationTable.userUuid] = userUuid
                it[DeviceLocationTable.provider] = deviceLocation.provider
                it[DeviceLocationTable.fixTime] = deviceLocation.fixTime
                it[DeviceLocationTable.deviceTime] = deviceLocation.deviceTime
                it[DeviceLocationTable.latitude] = deviceLocation.latitude
                it[DeviceLocationTable.longitude] = deviceLocation.longitude
                it[DeviceLocationTable.altitude] = deviceLocation.altitude
                it[DeviceLocationTable.accuracy] = deviceLocation.accuracy
                it[DeviceLocationTable.bearing] = deviceLocation.bearing
                it[DeviceLocationTable.speed] = deviceLocation.speed
            }.insertedCount > 0
        }.onFailure {
            Telemetry.error(tag = TAG, message = "Unable to record location for user $userUuid", throwable = it)
        }.getOrDefault(defaultValue = false)
    }

    /**
     * Maps a database row to a [DeviceLocation] domain model.
     *
     * @receiver A row from [DeviceLocationTable].
     * @return The mapped [DeviceLocation].
     */
    private fun ResultRow.toDeviceLocation(): DeviceLocation = DeviceLocation(
        uuid = this[DeviceLocationTable.uuid],
        userUuid = this[DeviceLocationTable.userUuid],
        provider = this[DeviceLocationTable.provider],
        fixTime = this[DeviceLocationTable.fixTime],
        deviceTime = this[DeviceLocationTable.deviceTime],
        latitude = this[DeviceLocationTable.latitude],
        longitude = this[DeviceLocationTable.longitude],
        altitude = this[DeviceLocationTable.altitude],
        accuracy = this[DeviceLocationTable.accuracy],
        bearing = this[DeviceLocationTable.bearing],
        speed = this[DeviceLocationTable.speed],
    )

    companion object {
        private const val TAG = "DeviceLocationGateway"
    }
}
