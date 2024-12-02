package com.app.builder.ui.core.button

import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentSetOf
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FormatBold
import androidx.compose.material.icons.filled.FormatItalic
import androidx.compose.material.icons.filled.FormatUnderlined
import androidx.compose.material3.MultiChoiceSegmentedButtonRow
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.app.builder.ui.LocalTypography
import com.app.builder.ui.Preview
import com.app.builder.ui.core.image.Icon
import com.app.builder.ui.core.text.Text
import androidx.compose.material3.SegmentedButton as MaterialSegmentedButton

/** Distinguishes between the container treatments a [ButtonGroup] can adopt. */
enum class ButtonGroupVariant {
    /** Individually spaced buttons with no shared container, for actions that are related but not mutually exclusive. */
    STANDARD,
    /** A single connected surface for choosing from a small, related set. */
    CONNECTED
}

/**
 * Button group item.
 *
 * @property enabled Whether a specific item can be selected. Defaults to `true`.
 * @property selected Whether a specific item is currently selected. Defaults to `false`.
 * @property text The text label string for a given item. Defaults to null.
 * @property icon An optional [ImageVector] rendered alongside the text. In [ButtonGroupVariant.CONNECTED], it crossfades with the selected checkmark and when null, the default checkmark-only behavior applies.
 * @property onClick A callback invoked when the item is selected by the user.
 */
@Stable
data class ButtonGroupItem(
    val enabled: Boolean = true,
    val selected: Boolean = false,
    val text: String? = null,
    val icon: ImageVector? = null,
    val onClick: () -> Unit = {}
)

/**
 * A row of related actions.
 *
 * @param modifier The [Modifier] to be applied to the row layout.
 * @param variant The container treatment to render. Defaults to [ButtonGroupVariant.CONNECTED].
 * @param multiSelect Whether more than one item may be selected at once. Only meaningful for [ButtonGroupVariant.CONNECTED] and ignored for [ButtonGroupVariant.STANDARD], where every button is already independently toggleable.
 * @param items An [ImmutableList] of [ButtonGroupItem]s for each option.
 */
@Composable
fun ButtonGroup(
    modifier: Modifier = Modifier,
    variant: ButtonGroupVariant = ButtonGroupVariant.CONNECTED,
    multiSelect: Boolean = false,
    items: ImmutableList<ButtonGroupItem> = persistentListOf(),
) {
    val typography = LocalTypography.current
    val label: @Composable (ButtonGroupItem) -> Unit = { item ->
        Text(
            text = item.text.orEmpty(),
            maxLines = 1,
            style = typography.labelLarge
        )
    }

    when (variant) {
        ButtonGroupVariant.STANDARD -> Row(
            modifier = modifier,
            horizontalArrangement = Arrangement.spacedBy(space = 8.dp)
        ) {
            items.forEach { item ->
                Button(
                    style = if (item.selected) ButtonStyle.FILLED else ButtonStyle.OUTLINED,
                    enabled = item.enabled,
                    onClick = { item.onClick() },
                    text = item.text,
                    content = item.icon?.let { icon -> { Icon(imageVector = icon) } }
                )
            }
        }

        ButtonGroupVariant.CONNECTED -> if (!multiSelect) {
            SingleChoiceSegmentedButtonRow(modifier = modifier) {
                items.forEachIndexed { index, item ->
                    MaterialSegmentedButton(
                        shape = SegmentedButtonDefaults.itemShape(index = index, count = items.size),
                        selected = item.selected,
                        enabled = item.enabled,
                        onClick = item.onClick,
                        icon = {
                            SegmentedButtonDefaults.Icon(
                                active = item.selected,
                                inactiveContent = item.icon?.let { icon -> { Icon(imageVector = icon) } }
                            )
                        },
                        label = { label(item) }
                    )
                }
            }
        } else {
            MultiChoiceSegmentedButtonRow(modifier = modifier) {
                items.forEachIndexed { index, item ->
                    MaterialSegmentedButton(
                        shape = SegmentedButtonDefaults.itemShape(index = index, count = items.size),
                        checked = item.selected,
                        enabled = item.enabled,
                        onCheckedChange = { item.onClick() },
                        icon = {
                            SegmentedButtonDefaults.Icon(
                                active = item.selected,
                                inactiveContent = item.icon?.let { icon -> { Icon(imageVector = icon) } }
                            )
                        },
                        label = { label(item) }
                    )
                }
            }
        }
    }
}

private val items = listOf(
    "Bold" to Icons.Default.FormatBold,
    "Italic" to Icons.Default.FormatItalic,
    "Underline" to Icons.Default.FormatUnderlined
)

@Preview
@Composable
private fun ButtonGroupPreview() = Preview {
    var selectedIndex by remember { mutableIntStateOf(value = 0) }
    ButtonGroup(
        variant = ButtonGroupVariant.STANDARD,
        items = persistentListOf(
            *items.mapIndexed { index, (label, icon) ->
                ButtonGroupItem(
                    text = label,
                    icon = icon,
                    selected = index == selectedIndex,
                    onClick = { selectedIndex = index }
                )
            }.toTypedArray()
        )
    )
}

@Preview
@Composable
private fun ButtonGroupDisabledPreview() = Preview {
    ButtonGroup(
        variant = ButtonGroupVariant.STANDARD,
        items = persistentListOf(
            *items.mapIndexed { index, (label, icon) ->
                ButtonGroupItem(
                    text = label,
                    icon = icon,
                    enabled = false,
                    selected = index == 0,
                )
            }.toTypedArray()
        )
    )
}

@Preview
@Composable
private fun ButtonGroupConnectedPreview() = Preview {
    var selectedIndex by remember { mutableIntStateOf(value = 0) }
    ButtonGroup(
        variant = ButtonGroupVariant.CONNECTED,
        items = persistentListOf(
            *items.mapIndexed { index, (label, icon) ->
                ButtonGroupItem(
                    text = label,
                    icon = icon,
                    selected = index == selectedIndex,
                    onClick = { selectedIndex = index }
                )
            }.toTypedArray()
        )
    )
}

@Preview
@Composable
private fun ButtonGroupConnectedDisabledPreview() = Preview {
    ButtonGroup(
        variant = ButtonGroupVariant.CONNECTED,
        items = persistentListOf(
            *items.mapIndexed { index, (label, icon) ->
                ButtonGroupItem(
                    text = label,
                    icon = icon,
                    enabled = false,
                    selected = index == 0,
                )
            }.toTypedArray()
        )
    )
}

@Preview
@Composable
private fun ButtonGroupMultiSelectPreview() = Preview {
    var selectedIndices by remember { mutableStateOf(value = persistentSetOf(0, 1)) }
    ButtonGroup(
        variant = ButtonGroupVariant.CONNECTED,
        multiSelect = true,
        items = persistentListOf(
            *items.mapIndexed { index, (label, icon) ->
                ButtonGroupItem(
                    text = label,
                    icon = icon,
                    selected = index in selectedIndices,
                    onClick = { selectedIndices = if (index in selectedIndices) selectedIndices.removing(element = index) else selectedIndices.adding(element = index) }
                )
            }.toTypedArray()
        )
    )
}

@Preview
@Composable
private fun ButtonGroupMultiSelectDisabledPreview() = Preview {
    ButtonGroup(
        variant = ButtonGroupVariant.CONNECTED,
        multiSelect = true,
        items = persistentListOf(
            *items.mapIndexed { index, (label, icon) ->
                ButtonGroupItem(
                    text = label,
                    icon = icon,
                    enabled = false,
                    selected = index == 0 || index == 1,
                )
            }.toTypedArray()
        )
    )
}
