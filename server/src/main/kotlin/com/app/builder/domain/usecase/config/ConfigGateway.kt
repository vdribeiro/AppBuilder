package com.app.builder.domain.usecase.config

import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withContext
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.r2dbc.R2dbcDatabase
import org.jetbrains.exposed.v1.r2dbc.selectAll
import org.jetbrains.exposed.v1.r2dbc.upsert
import com.app.builder.core.config.ClientConfigs
import com.app.builder.core.config.ClientFlags
import com.app.builder.core.config.ServerConfigs
import com.app.builder.core.config.ServerFlags
import com.app.builder.core.flow.Dispatcher
import com.app.builder.core.telemetry.Telemetry
import com.app.builder.data.database.safeTransaction
import com.app.builder.data.database.table.ConfigTable
import com.app.builder.data.serializer.decode
import com.app.builder.data.serializer.encode

/**
 * Gateway implementation for config use cases.
 *
 * @property database The database instance used to execute transactions.
 */
class ConfigGateway(
    private val database: R2dbcDatabase
): ConfigUseCases {

    override suspend fun load() = withContext(context = Dispatcher.IO) {
        load<ClientFlags>(key = ConfigTable.Key.CLIENT_FLAG)?.run { ClientFlags.set { this } }
        load<ClientConfigs>(key = ConfigTable.Key.CLIENT_CONFIG)?.run { ClientConfigs.set { this } }
        load<ServerFlags>(key = ConfigTable.Key.SERVER_FLAG)?.run { ServerFlags.set { this } }
        load<ServerConfigs>(key = ConfigTable.Key.SERVER_CONFIG)?.run { ServerConfigs.set { this } }
        Unit
    }

    override suspend fun updateClientFeatureFlags(flags: ClientFlags): Boolean = withContext(context = Dispatcher.IO) {
        update(key = ConfigTable.Key.CLIENT_FLAG, value = flags).also { ClientFlags.set { flags } }
    }

    override suspend fun updateClientConfigs(configs: ClientConfigs): Boolean = withContext(context = Dispatcher.IO) {
        update(key = ConfigTable.Key.CLIENT_CONFIG, value = configs).also { ClientConfigs.set { configs } }
    }

    override suspend fun updateServerFeatureFlags(flags: ServerFlags): Boolean = withContext(context = Dispatcher.IO) {
        update(key = ConfigTable.Key.SERVER_FLAG, value = flags).also { ServerFlags.set { flags } }
    }

    override suspend fun updateServerConfigs(configs: ServerConfigs): Boolean = withContext(context = Dispatcher.IO) {
        update(key = ConfigTable.Key.SERVER_CONFIG, value = configs).also { ServerConfigs.set { configs } }
    }

    /**
     * Loads a configuration value from the database.
     *
     * @param key The key of the configuration to load.
     * @return The loaded configuration value, or `null` if not found.
     */
    private suspend inline fun <reified C> load(key: ConfigTable.Key): C? =
        database.safeTransaction {
            ConfigTable
                .selectAll()
                .where { ConfigTable.key eq key }
                .firstOrNull()
                ?.get(expression = ConfigTable.value)
        }.onFailure {
            Telemetry.error(tag = TAG, message = "Unable to get $key", throwable = it)
        }.getOrNull()?.let { decode<C>(value = it) }

    /**
     * Updates a configuration value in the database.
     *
     * @param key The key of the configuration to update.
     * @param value The new value for the configuration.
     * @return `true` if the update was successful, `false` otherwise.
     */
    private suspend inline fun <reified C> update(key: ConfigTable.Key, value: C): Boolean {
        val value = encode(value = value) ?: run {
            Telemetry.error(tag = TAG, message = "Unable to encode $key")
            return false
        }
        return database.safeTransaction {
            ConfigTable.upsert {
                it[ConfigTable.key] = key
                it[ConfigTable.value] = value
            }.insertedCount > 0
        }.onFailure {
            Telemetry.error(tag = TAG, message = "Unable to upsert $key", throwable = it)
        }.getOrDefault(defaultValue = false)
    }

    companion object {
        private const val TAG = "ConfigGateway"
    }
}