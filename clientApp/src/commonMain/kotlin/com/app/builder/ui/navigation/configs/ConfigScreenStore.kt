package com.app.builder.ui.navigation.configs

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
import com.app.builder.domain.Permission
import com.app.builder.domain.gateway.authentication.AuthenticationUseCases
import com.app.builder.domain.gateway.config.ConfigUseCases
import com.app.builder.ui.component.list.ConfigItem
import com.app.builder.ui.component.list.ConfigValue
import com.app.builder.ui.store.Store

/**
 * Store backing the config screen.
 *
 * @param state The initial [ConfigScreenState].
 * @property configUseCases The use cases used to read and persist flags/configs.
 * @property authenticationUseCases The use cases used to observe the current user and their permissions.
 */
class ConfigScreenStore(
    state: ConfigScreenState,
    private val configUseCases: ConfigUseCases,
    private val authenticationUseCases: AuthenticationUseCases,
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
     * Observes the current user's permissions and loads one section per config type they can read, editable only where they can write.
     * Also observes the client flags and client configs, and refreshes their sections as they change.
     *
     * @return The [Job] representing this execution.
     */
    private fun setup(): Job = launch(id = "setup") {
        Telemetry.info(tag = TAG, message = "Setup")

        authenticationUseCases.observeCurrentAccount().observe(id = "current_account") { account ->
            account ?: return@observe

            val sections = ConfigType.entries
                .filter { account.hasPermission(entityType = it.entityType, permission = Permission.READ) }
                .map {
                    ConfigSection(
                        configType = it,
                        items = load(configType = it),
                        editable = account.hasPermission(entityType = it.entityType, permission = Permission.WRITE)
                    )
                }
                .toPersistentList()
            updateState { it.copy(sections = sections) }

            Telemetry.info(tag = TAG, message = "Loaded ${sections.size} sections")
        }

        ClientFlags.flow.observe(id = "client_flags") { flags ->
            val items = flags.toConfigItems(map = Configs.clientFlags) { ConfigValue.Toggle(value = it.boolean) }
            updateState { it.refreshItems(configType = ConfigType.CLIENT_FLAG, items = items) }
        }

        ClientConfigs.flow.observe(id = "client_configs") { configs ->
            val items = configs.toConfigItems(map = Configs.clientConfigs) { ConfigValue.Form(value = it.content) }
            updateState { it.refreshItems(configType = ConfigType.CLIENT_CONFIG, items = items) }
        }

        Telemetry.info(tag = TAG, message = "Setup complete")
    }

    /**
     * Loads the config items matching [configType].
     *
     * @param configType The config type to load.
     * @return The entries of that config type, empty when they could not be read.
     */
    private suspend fun load(configType: ConfigType): ImmutableList<ConfigItem> = when (configType) {
        ConfigType.CLIENT_FLAG -> ClientFlags.value.toConfigItems(map = Configs.clientFlags) { ConfigValue.Toggle(value = it.boolean) }
        ConfigType.CLIENT_CONFIG -> ClientConfigs.value.toConfigItems(map = Configs.clientConfigs) { ConfigValue.Form(value = it.content) }
        ConfigType.SERVER_FLAG -> configUseCases.getServerFeatureFlags()?.toConfigItems(map = Configs.serverFlags) { ConfigValue.Toggle(value = it.boolean) }
        ConfigType.SERVER_CONFIG -> configUseCases.getServerConfigs()?.toConfigItems(map = Configs.serverConfigs) { ConfigValue.Form(value = it.content) }
    }.orEmpty().toPersistentList()

    /**
     * Applies the new value to local state immediately, then debounces persistence.
     *
     * @param state The current state.
     * @param action The [ConfigScreenAction.UpdateValue] action.
     * @return The [Job] representing this execution.
     */
    private fun updateValue(state: ConfigScreenState, action: ConfigScreenAction.UpdateValue): Job = launch(id = "persist-${action.item.uuid}", replace = true) {
        Telemetry.info(tag = TAG, message = "Update Value")

        val persistable = action.value !is ConfigValue.Form || action.value.value.toDoubleOrNull() != null

        val items = state.sections
            .firstOrNull { it.configType == action.configType && it.editable }
            ?.items
            ?.map {
                if (it.uuid == action.item.uuid) it.copy(
                    pending = persistable,
                    value = action.value
                ) else it
            }
            ?.toPersistentList()
            ?: return@launch Telemetry.info(tag = TAG, message = "No editable ${action.configType} section to update")
        val pendingState = state.withItems(configType = action.configType, items = items)
        updateState { pendingState }

        if (!persistable) return@launch Telemetry.info(tag = TAG, message = "Waiting for a numeric form value before persisting")

        delay(timeMillis = ClientConfigs.value.inputDebounceMillis)

        when (action.configType) {
            ConfigType.CLIENT_FLAG -> items.toConfig<ClientFlags>()?.let { configUseCases.updateFeatureFlags(flags = it) }
            ConfigType.CLIENT_CONFIG -> items.toConfig<ClientConfigs>()?.let { configUseCases.updateConfigs(configs = it) }
            ConfigType.SERVER_FLAG -> items.toConfig<ServerFlags>()?.let { configUseCases.updateServerFeatureFlags(flags = it) }
            ConfigType.SERVER_CONFIG -> items.toConfig<ServerConfigs>()?.let { configUseCases.updateServerConfigs(configs = it) }
        }

        val finishedItems = items.map {
            if (it.uuid == action.item.uuid) it.copy(pending = false) else it
        }.toPersistentList()
        val finishedState = state.withItems(configType = action.configType, items = finishedItems)
        updateState { finishedState }

        Telemetry.info(tag = TAG, message = "Persisted ${action.item} with new value ${action.value}")
    }

    /**
     * Replaces the entries of the [configType] section, leaving every other section untouched.
     *
     * @param configType The config type whose section is being replaced.
     * @param items The new entries of that section.
     * @return The updated state.
     */
    private fun ConfigScreenState.withItems(configType: ConfigType, items: ImmutableList<ConfigItem>): ConfigScreenState =
        copy(sections = sections.map { if (it.configType == configType) it.copy(items = items) else it }.toPersistentList())

    /**
     * Sets the values of the [configType] section to those of [items], matched by name. Entries with a pending edit keep their value.
     *
     * @param configType The config type whose section is being refreshed.
     * @param items The entries holding the new values.
     * @return The updated state.
     */
    private fun ConfigScreenState.refreshItems(configType: ConfigType, items: List<ConfigItem>): ConfigScreenState {
        val values = items.associate { it.name to it.value }
        return copy(sections = sections.map { section ->
            if (section.configType != configType) section else section.copy(
                items = section.items.map { item ->
                    val value = values[item.name]
                    if (item.pending || value == null) item else item.copy(value = value)
                }.toPersistentList()
            )
        }.toPersistentList())
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
    }
}
