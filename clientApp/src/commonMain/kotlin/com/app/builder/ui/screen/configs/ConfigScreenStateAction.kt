package com.app.builder.ui.screen.configs

import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import com.app.builder.ui.component.list.ConfigItem
import com.app.builder.ui.component.list.ConfigValue

/** Actions dispatched by the config UI. */
sealed interface ConfigScreenAction {
    /**
     * Updates a single [ConfigItem] to the given [value].
     *
     * @param item Config item being updated.
     * @param value New value for the config item.
     */
    data class UpdateValue(val item: ConfigItem, val value: ConfigValue): ConfigScreenAction
}

/**
 * State for a config screen, holding the items currently displayed.
 *
 * @property items Config items currently displayed.
 * @property configType Which set of client or server flags/configs this store manages.
 */
data class ConfigScreenState(
    val items: ImmutableList<ConfigItem> = persistentListOf(),
    val configType: ConfigType
)

/** Identifies which set of client or server flags/configs a [ConfigScreenStore] manages. */
enum class ConfigType {
    /** Client feature flags. */
    CLIENT_FLAG,
    /** Client configs. */
    CLIENT_CONFIG,
    /** Server feature flags. */
    SERVER_FLAG,
    /** Server configs. */
    SERVER_CONFIG
}
