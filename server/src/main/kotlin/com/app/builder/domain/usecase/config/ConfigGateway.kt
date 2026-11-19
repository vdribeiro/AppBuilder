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
import com.app.builder.data.signal.InstanceSignal
import com.app.builder.domain.usecase.Outcome
import com.app.builder.domain.usecase.toOutcome

/**
 * Gateway implementation for config use cases.
 *
 * @property database The database instance used to execute transactions.
 * @property instanceSignal Signals config updates to every server instance.
 */
class ConfigGateway(
    private val database: R2dbcDatabase,
    private val instanceSignal: InstanceSignal
): ConfigUseCases {

    override suspend fun load() = withContext(context = Dispatcher.IO) {
        ConfigTable.Key.entries.forEach { load(key = it) }
    }

    override suspend fun observe() = withContext(context = Dispatcher.IO) {
        instanceSignal.observe(channel = CHANNEL).collect { payload ->
            ConfigTable.Key.entries.find { it.name == payload }?.let { load(key = it) }
        }
    }

    override suspend fun updateClientFeatureFlags(flags: ClientFlags): Outcome<Unit> = withContext(context = Dispatcher.IO) {
        update(key = ConfigTable.Key.CLIENT_FLAG, value = flags).also { ClientFlags.set { flags } }
    }

    override suspend fun updateClientConfigs(configs: ClientConfigs): Outcome<Unit> = withContext(context = Dispatcher.IO) {
        update(key = ConfigTable.Key.CLIENT_CONFIG, value = configs).also { ClientConfigs.set { configs } }
    }

    override suspend fun updateServerFeatureFlags(flags: ServerFlags): Outcome<Unit> = withContext(context = Dispatcher.IO) {
        update(key = ConfigTable.Key.SERVER_FLAG, value = flags).also { ServerFlags.set { flags } }
    }

    override suspend fun updateServerConfigs(configs: ServerConfigs): Outcome<Unit> = withContext(context = Dispatcher.IO) {
        update(key = ConfigTable.Key.SERVER_CONFIG, value = configs).also { ServerConfigs.set { configs } }
    }

    /**
     * Restores the persisted config record of a key.
     *
     * @param key The key of the configuration to restore.
     */
    private suspend fun load(key: ConfigTable.Key) {
        when (key) {
            ConfigTable.Key.CLIENT_FLAG -> read<ClientFlags>(key = key)?.run { ClientFlags.set { this } }
            ConfigTable.Key.CLIENT_CONFIG -> read<ClientConfigs>(key = key)?.run { ClientConfigs.set { this } }
            ConfigTable.Key.SERVER_FLAG -> read<ServerFlags>(key = key)?.run { ServerFlags.set { this } }
            ConfigTable.Key.SERVER_CONFIG -> read<ServerConfigs>(key = key)?.run { ServerConfigs.set { this } }
        }
    }

    /**
     * Reads a configuration value from the database.
     *
     * @param key The key of the configuration to read.
     * @return The configuration value, or `null` if not found.
     */
    private suspend inline fun <reified C> read(key: ConfigTable.Key): C? =
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
     * Updates a configuration value in the database and signals the update to every server instance.
     *
     * @param key The key of the configuration to update.
     * @param value The new value for the configuration.
     * @return [Outcome.Success] when the update was persisted, or [Outcome.Error] otherwise.
     */
    private suspend inline fun <reified C> update(key: ConfigTable.Key, value: C): Outcome<Unit> {
        val value = encode(value = value) ?: run {
            Telemetry.error(tag = TAG, message = "Unable to encode $key")
            return Outcome.Error
        }
        val updated = database.safeTransaction {
            ConfigTable.upsert {
                it[ConfigTable.key] = key
                it[ConfigTable.value] = value
            }.insertedCount > 0
        }.onFailure {
            Telemetry.error(tag = TAG, message = "Unable to upsert $key", throwable = it)
        }.getOrDefault(defaultValue = false)
        if (updated && !instanceSignal.notify(channel = CHANNEL, payload = key.name)) {
            Telemetry.error(tag = TAG, message = "Unable to signal $key to other instances")
        }
        return updated.toOutcome()
    }

    companion object {
        private const val TAG = "ConfigGateway"

        /** The signal channel relaying the key of each updated config record. */
        internal const val CHANNEL = "config"
    }
}