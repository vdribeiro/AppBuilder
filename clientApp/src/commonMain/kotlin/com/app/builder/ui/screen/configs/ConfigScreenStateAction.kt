package com.app.builder.ui.screen.configs

import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.runtime.Stable
import androidx.compose.ui.graphics.vector.ImageVector
import com.app.builder.domain.EntityType
import com.app.builder.ui.component.list.ConfigItem
import com.app.builder.ui.component.list.ConfigValue

/** Actions dispatched by the config UI. */
sealed interface ConfigScreenAction {
    /**
     * Updates a single [ConfigItem] to the given [value].
     *
     * @param configType Config type the item belongs to.
     * @param item Config item being updated.
     * @param value New value for the config item.
     */
    data class UpdateValue(
        val configType: ConfigType,
        val item: ConfigItem,
        val value: ConfigValue
    ): ConfigScreenAction
}

/**
 * State for the config screen, holding every section the current user may browse, stacked as one scrollable page.
 *
 * @property sections Config sections currently displayed, in [ConfigType] order.
 */
data class ConfigScreenState(
    val sections: ImmutableList<ConfigSection> = persistentListOf()
)

/**
 * One section of the config screen, holding the entries of a single [ConfigType].
 *
 * @property configType Which set of client or server flags/configs this section holds.
 * @property items Config entries of that type.
 */
@Stable
data class ConfigSection(
    val configType: ConfigType,
    val items: ImmutableList<ConfigItem> = persistentListOf()
)

/**
 * Identifies one set of client or server flags/configs, rendered as its own section of the config screen.
 *
 * @property text The navigation rail label for this config type.
 * @property icon The navigation rail icon for this config type.
 * @property entityType The [EntityType] whose permission grants access to this config type.
 */
enum class ConfigType(
    val text: String,
    val icon: ImageVector,
    val entityType: EntityType
) {
    /** Client feature flags. */
    CLIENT_FLAG(text = "client_flag", icon = Icons.Filled.Flag, entityType = EntityType.CLIENT_FLAG),
    /** Client configs. */
    CLIENT_CONFIG(text = "client_config", icon = Icons.Filled.Settings, entityType = EntityType.CLIENT_CONFIG),
    /** Server feature flags. */
    SERVER_FLAG(text = "server_flag", icon = Icons.Outlined.Flag, entityType = EntityType.SERVER_FLAG),
    /** Server configs. */
    SERVER_CONFIG(text = "server_config", icon = Icons.Outlined.Settings, entityType = EntityType.SERVER_CONFIG)
}
