package com.app.builder.domain.usecase.devicetoken

import kotlin.uuid.Uuid
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.withContext
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.r2dbc.R2dbcDatabase
import org.jetbrains.exposed.v1.r2dbc.deleteWhere
import org.jetbrains.exposed.v1.r2dbc.selectAll
import org.jetbrains.exposed.v1.r2dbc.upsert
import com.app.builder.core.flow.Dispatcher
import com.app.builder.core.telemetry.Telemetry
import com.app.builder.data.database.safeTransaction
import com.app.builder.data.database.table.DeviceTokenTable
import com.app.builder.domain.DeviceToken

/**
 * Gateway implementation for device token use cases.
 *
 * @property database The database instance used to execute transactions.
 */
class DeviceTokenGateway(
    private val database: R2dbcDatabase
): DeviceTokenUseCases {

    override suspend fun registerToken(userUuid: Uuid, registration: DeviceToken): Boolean = withContext(context = Dispatcher.IO) {
        database.safeTransaction {
            DeviceTokenTable.upsert {
                it[DeviceTokenTable.deviceUuid] = registration.deviceUuid!!
                it[DeviceTokenTable.userUuid] = userUuid
                it[DeviceTokenTable.token] = registration.token
            }.insertedCount > 0
        }.onFailure {
            Telemetry.error(tag = TAG, message = "Unable to register device token for user $userUuid", throwable = it)
        }.getOrDefault(defaultValue = false)
    }

    override suspend fun getTokens(userUuid: Uuid): List<DeviceToken> = withContext(context = Dispatcher.IO) {
        database.safeTransaction {
            DeviceTokenTable
                .selectAll()
                .where { DeviceTokenTable.userUuid eq userUuid }
                .toList()
                .map { DeviceToken(deviceUuid = it[DeviceTokenTable.deviceUuid], token = it[DeviceTokenTable.token]) }
        }.onFailure {
            Telemetry.error(tag = TAG, message = "Unable to get device tokens for user $userUuid", throwable = it)
        }.getOrDefault(defaultValue = emptyList())
    }

    override suspend fun removeToken(token: String): Boolean = withContext(context = Dispatcher.IO) {
        database.safeTransaction {
            DeviceTokenTable.deleteWhere { DeviceTokenTable.token eq token } > 0
        }.onFailure {
            Telemetry.error(tag = TAG, message = "Unable to remove device token", throwable = it)
        }.getOrDefault(defaultValue = false)
    }

    companion object {
        private const val TAG = "DeviceTokenGateway"
    }
}
