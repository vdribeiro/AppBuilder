package com.app.builder.ui.component.list

import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.app.builder.core.security.uuid
import com.app.builder.ui.Preview
import com.app.builder.ui.core.button.Switch
import com.app.builder.ui.core.list.ListItem
import com.app.builder.ui.core.progress.ProgressIndicator
import com.app.builder.ui.core.text.Input

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
 * @param enabled Whether the user can edit the entries.
 * @param onValueChange Callback invoked with the edited item and value when the user makes a change.
 */
@Composable
fun ConfigList(
    modifier: Modifier = Modifier,
    items: ImmutableList<ConfigItem> = persistentListOf(),
    enabled: Boolean = true,
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
            ListItem(
                modifier = Modifier.testTag(tag = "config_row_$index"),
                selected = item.pending,
                translate = false,
                headlineText = item.name.toLabel(),
                supportingText = item.description.takeIf { it.isNotBlank() },
                trailingContent = {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(space = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        AnimatedVisibility(visible = item.pending, enter = fadeIn(), exit = fadeOut()) {
                            ProgressIndicator(modifier = Modifier.size(size = 16.dp))
                        }
                        when (item.value) {
                            is ConfigValue.Toggle -> Switch(
                                checked = item.value.value,
                                enabled = enabled,
                                onCheckedChange = { onValueChange(item, ConfigValue.Toggle(value = it)) }
                            )

                            is ConfigValue.Form -> Input(
                                modifier = Modifier.width(width = 160.dp),
                                maxLines = 1,
                                value = item.value.value,
                                enabled = enabled,
                                onValueChange = { onValueChange(item, ConfigValue.Form(value = it)) }
                            )
                        }
                    }
                }
            )
        }
    }
}

/**
 * Converts a camelCase config field name into a human-readable label, splitting it on each capital and capitalizing the first letter.
 *
 * @receiver The raw config field name, as declared in the flags/configs class.
 * @return The label to display, so `featureFlag` reads as `Feature Flag`.
 */
private fun String.toLabel(): String = buildString {
    this@toLabel.forEachIndexed { index, char ->
        when {
            index == 0 -> append(char.uppercaseChar())
            char.isUpperCase() -> append(' ').append(char)
            else -> append(char)
        }
    }
}

@PreviewLightDark
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
