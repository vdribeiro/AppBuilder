package com.app.builder.ui.screen.configs

import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import com.app.builder.Configs
import com.app.builder.core.config.ClientConfigs
import com.app.builder.core.config.ClientFlags
import com.app.builder.core.config.ServerConfigs
import com.app.builder.core.config.ServerFlags
import com.app.builder.core.telemetry.Telemetry
import com.app.builder.data.serializer.decodeFromJson
import com.app.builder.data.serializer.encodeToJson
import com.app.builder.domain.gateway.config.ConfigUseCases
import com.app.builder.ui.component.list.ConfigItem
import com.app.builder.ui.component.list.ConfigValue
import com.app.builder.ui.store.Store

/**
 * Store backing a config list screen for a given config type, loading the relevant flags/configs and persisting changes made through [ConfigScreenAction.UpdateValue].
 *
 * @param state The initial [ConfigScreenState].
 * @property configUseCases The use cases used to read and persist flags/configs.
 */
class ConfigScreenStore(
    state: ConfigScreenState,
    private val configUseCases: ConfigUseCases,
): Store<ConfigScreenState, ConfigScreenAction>(initialState = state) {
    init {
        setup()
    }

    override fun reducer(state: ConfigScreenState, action: ConfigScreenAction) {
        super.reducer(state = state, action = action)
        when (action) {
            is ConfigScreenAction.UpdateValue -> updateValue(state = state, action = action)
        }
    }

    /**
     * Loads the config items matching the configType and updates state with the resulting list.
     *
     * @return The [Job] representing this execution.
     */
    private fun setup(): Job = launch(id = "setup") {
        Telemetry.info(tag = TAG, message = "Setup")

        val items = when (state.configType) {
            ConfigType.CLIENT_FLAG -> ClientFlags.flags.toConfigItems(map = Configs.clientFlags) { ConfigValue.Toggle(value = it.boolean) }
            ConfigType.CLIENT_CONFIG -> ClientConfigs.configs.toConfigItems(map = Configs.clientConfigs) { ConfigValue.Form(value = it.content) }
            ConfigType.SERVER_FLAG -> configUseCases.getServerFeatureFlags()?.toConfigItems(map = Configs.serverFlags) { ConfigValue.Toggle(value = it.boolean) }
            ConfigType.SERVER_CONFIG -> configUseCases.getServerConfigs()?.toConfigItems(map = Configs.serverConfigs) { ConfigValue.Form(value = it.content) }
        }.orEmpty().toPersistentList()

        updateState { it.copy(items = items) }

        Telemetry.info(tag = TAG, message = "Setup complete")
    }

    /**
     * Applies the new value to local state immediately, then debounces persistence.
     *
     * @param state The current state.
     * @param action The [ConfigScreenAction.UpdateValue] action.
     * @return The [Job] representing this execution.
     */
    private fun updateValue(state: ConfigScreenState, action: ConfigScreenAction.UpdateValue): Job = launch(id = "persist-${action.item.uuid}", replace = true) {
        Telemetry.info(tag = TAG, message = "Update Value")

        // Validate input: Form values must be numbers for now
        if (action.value is ConfigValue.Form && action.value.value.toLongOrNull() == null) return@launch Telemetry.error(tag = TAG, message = "Invalid form value")

        val items = state.items.map {
            if (it.uuid == action.item.uuid) it.copy(
                pending = true,
                value = action.value
            ) else it
        }.toPersistentList()
        updateState { it.copy(items = items) }

        delay(timeMillis = DEBOUNCE_MILLIS)

        when (state.configType) {
            ConfigType.CLIENT_FLAG -> items.toConfig<ClientFlags>()?.let { configUseCases.updateFeatureFlags(flags = it) }
            ConfigType.CLIENT_CONFIG -> items.toConfig<ClientConfigs>()?.let { configUseCases.updateConfigs(configs = it) }
            ConfigType.SERVER_FLAG -> items.toConfig<ServerFlags>()?.let { configUseCases.updateServerFeatureFlags(flags = it) }
            ConfigType.SERVER_CONFIG -> items.toConfig<ServerConfigs>()?.let { configUseCases.updateServerConfigs(configs = it) }
        }

        val finishedItems = state.items.map {
            if (it.uuid == action.item.uuid) it.copy(
                pending = false,
                value = action.value
            ) else it
        }.toPersistentList()
        updateState { it.copy(items = finishedItems) }

        Telemetry.info(tag = TAG, message = "Persisted ${action.item} with new value ${action.value}")
    }

    /**
     * Converts this serializable value into one [ConfigItem] per field, using [map] to look up each field's description and [toValue] to build its value.
     *
     * @param map Map of field name to its display description.
     * @param toValue Converts a field's JSON value into a [ConfigValue].
     * @return The list of config items, or an empty list if this value could not be encoded.
     */
    private inline fun <reified T> T.toConfigItems(
        map: Map<String, String>,
        toValue: (JsonPrimitive) -> ConfigValue
    ): ImmutableList<ConfigItem> = encodeToJson(value = this)?.jsonObject?.map { (name, value) ->
        ConfigItem(
            name = name,
            description = map[name].orEmpty(),
            value = toValue(value.jsonPrimitive)
        )
    }.orEmpty().toPersistentList()

    /**
     * Converts this list of config items back into an instance of [T] by encoding each item's value as a JSON field keyed by its name.
     *
     * @return The decoded config, or `null` if decoding failed.
     */
    private inline fun <reified T> List<ConfigItem>.toConfig(): T? =
        decodeFromJson<T>(value = JsonObject(content = associate { config ->
            config.name to config.value.let {
                when (it) {
                    is ConfigValue.Toggle -> JsonPrimitive(value = it.value)
                    is ConfigValue.Form -> JsonPrimitive(value = it.value)
                }
            }
        }))

    companion object {
        private const val TAG = "ConfigScreenStore"

        /** Delay before persisting a config value change, to debounce rapid successive edits. */
        private const val DEBOUNCE_MILLIS = 1000L
    }
}