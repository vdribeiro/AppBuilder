package com.app.builder.ui.component.field

import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.app.builder.ui.LocalColorScheme
import com.app.builder.ui.LocalTypography
import com.app.builder.ui.Preview
import com.app.builder.ui.core.button.ButtonGroup
import com.app.builder.ui.core.button.ButtonGroupItem
import com.app.builder.ui.core.button.Chip
import com.app.builder.ui.core.button.Dropdown
import com.app.builder.ui.core.button.DropdownItem
import com.app.builder.ui.core.image.Icon
import com.app.builder.ui.core.text.Text

/**
 * A property with a fixed set of options. While [editing], up to [maxSegments] options show as a connected button group and more than that as a dropdown.
 * Otherwise, the selected option shows as a [FieldValue].
 *
 * @param modifier The [Modifier] to be applied to the root layout.
 * @param label The label of the field, translated.
 * @param selected The selected option, or `null` if none is.
 * @param options Every option, as translation keys.
 * @param editing Whether the selection can be changed.
 * @param onSelect Callback for when an option is selected.
 * @param maxSegments The most options shown as a button group before switching to a dropdown.
 */
@Composable
fun ChoiceField(
    modifier: Modifier = Modifier,
    label: String = "",
    selected: String? = null,
    options: ImmutableList<String> = persistentListOf(),
    editing: Boolean = false,
    onSelect: (String) -> Unit = {},
    maxSegments: Int = 4,
) {
    val typography = LocalTypography.current
    val colorScheme = LocalColorScheme.current

    if (!editing) {
        FieldValue(modifier = modifier, label = label, value = selected, translateValue = true)
        return
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(space = 4.dp),
    ) {
        Text(
            text = label,
            maxLines = 1,
            style = typography.labelMedium,
            color = colorScheme.onSurfaceVariant
        )
        if (options.size <= maxSegments) ButtonGroup(
            items = options.map { option ->
                ButtonGroupItem(
                    selected = option == selected,
                    text = option,
                    onClick = { onSelect(option) }
                )
            }.toPersistentList()
        ) else Box {
            var expanded by remember { mutableStateOf(value = false) }
            Chip(
                text = selected,
                trailingIcon = { Icon(imageVector = Icons.Default.ArrowDropDown) },
                onClick = { expanded = true }
            )
            Dropdown(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                items = options.map { option ->
                    DropdownItem(
                        text = option,
                        onClick = {
                            expanded = false
                            onSelect(option)
                        }
                    )
                }.toPersistentList()
            )
        }
    }
}

@PreviewLightDark
@Composable
private fun ChoiceFieldPreview() = Preview {
    var selected by remember { mutableStateOf(value = "To do") }
    val options = persistentListOf("To do", "Complete")
    val manyOptions = persistentListOf("Low", "Medium", "High", "Urgent", "Blocker")
    Column(verticalArrangement = Arrangement.spacedBy(space = 8.dp)) {
        ChoiceField(label = "State", selected = selected, options = options)
        ChoiceField(label = "State", selected = selected, options = options, editing = true, onSelect = { selected = it })
        ChoiceField(label = "Priority", selected = "High", options = manyOptions, editing = true)
    }
}
