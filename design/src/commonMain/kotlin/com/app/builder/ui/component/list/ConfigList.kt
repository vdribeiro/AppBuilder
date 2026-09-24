package com.app.builder.ui.component.list

import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.tooling.preview.Preview
import com.app.builder.core.security.uuid
import com.app.builder.ui.Preview
import com.app.builder.ui.component.card.ConfigCardInput
import com.app.builder.ui.component.card.ConfigCardSwitch

/**
 * A config entry.
 *
 * @property uuid A unique identifier of the config. Defaults to a random UUID.
 * @property name The name of the config entry.
 * @property description A human-readable explanation of what the entry controls.
 * @property pending Whether an edit to this entry is currently being persisted.
 * @property value The current [ConfigValue].
 */
@Stable
data class ConfigItem(
    val uuid: String = uuid().toString(),
    val name: String = "",
    val description: String = "",
    val pending: Boolean = false,
    val value: ConfigValue = ConfigValue.Toggle(value = false),
)

/** A config value that is either a boolean or a free-form value. */
@Stable
sealed interface ConfigValue {
    /**
     * A boolean config value.
     *
     * @property value The current toggle state.
     */
    data class Toggle(val value: Boolean): ConfigValue

    /**
     * A free-form config value.
     *
     * @property value The current text value.
     */
    data class Form(val value: String): ConfigValue
}

/**
 * A list of flag or config entries.
 *
 * @param modifier The [Modifier] to be applied to the root layout.
 * @param items An [ImmutableList] of [ConfigItem]s.
 * @param onValueChange Callback invoked with the edited item and value when the user makes a change.
 */
@Composable
fun ConfigList(
    modifier: Modifier = Modifier,
    items: ImmutableList<ConfigItem> = persistentListOf(),
    onValueChange: (ConfigItem, ConfigValue) -> Unit = { _, _ -> }
) {
    Column(
        modifier = modifier
            .testTag(tag = "config_list")
            .fillMaxWidth(),
        horizontalAlignment = Alignment.Start,
        verticalArrangement = Arrangement.Top
    ) {
        items.forEachIndexed { index, item ->
            when (item.value) {
                is ConfigValue.Toggle -> ConfigCardSwitch(
                    modifier = Modifier.testTag(tag = "config_card_$index"),
                    name = item.name,
                    description = item.description,
                    value = item.value.value,
                    pending = item.pending,
                    onValueChange = { onValueChange(item, ConfigValue.Toggle(value = it)) }
                )

                is ConfigValue.Form -> ConfigCardInput(
                    modifier = Modifier.testTag(tag = "config_card_$index"),
                    name = item.name,
                    description = item.description,
                    value = item.value.value,
                    pending = item.pending,
                    onValueChange = { onValueChange(item, ConfigValue.Form(value = it)) }
                )
            }
        }
    }
}

@Preview
@Composable
private fun ConfigListPreview() = Preview {
    ConfigList(
        items = persistentListOf(
            ConfigItem(
                name = "http",
                description = "Toggles outbound networking.",
                value = ConfigValue.Toggle(value = true)
            ),
            ConfigItem(
                name = "httpConnectTimeoutMillis",
                description = "Maximum time in milliseconds allowed to establish a connection with the server.",
                value = ConfigValue.Form(value = "20000")
            ),
            ConfigItem(
                name = "http",
                description = "Toggles outbound networking.",
                value = ConfigValue.Toggle(value = true),
                pending = true
            ),
        )
    )
}
