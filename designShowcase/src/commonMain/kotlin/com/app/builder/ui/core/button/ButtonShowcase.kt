package com.app.builder.ui.core.button

import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentSetOf
import kotlinx.coroutines.delay
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FormatBold
import androidx.compose.material.icons.filled.FormatItalic
import androidx.compose.material.icons.filled.FormatUnderlined
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.app.builder.ShowcaseSection
import com.app.builder.ui.core.image.Icon
import com.app.builder.ui.core.text.Text

/** Displays buttons, fabs, button groups, checkboxes, radio buttons, switches, dropdowns, and chips in their various styles and states. */
@Composable
fun ButtonShowcase() {
    Column(
        modifier = Modifier.padding(all = 16.dp),
        verticalArrangement = Arrangement.spacedBy(space = 32.dp),
    ) {
        ShowcaseSection(title = "Text buttons") { ButtonPreview() }
        ShowcaseSection(title = "Icon buttons") { IconButtonPreview() }
        ShowcaseSection(title = "Toggle buttons") { ToggleButtonPreview() }
        ShowcaseSection(title = "Split Button") { Text(text = "Not implemented yet") }
        ShowcaseSection(title = "Fab") { FabPreview() }
        ShowcaseSection(title = "Fab Menu") { Text(text = "Not implemented yet") }
        ShowcaseSection(title = "Button group standard") { ButtonGroupPreview() }
        ShowcaseSection(title = "Button group standard disabled") { ButtonGroupDisabledPreview() }
        ShowcaseSection(title = "Button group connected") { ButtonGroupConnectedPreview() }
        ShowcaseSection(title = "Button group connected disabled") { ButtonGroupConnectedDisabledPreview() }
        ShowcaseSection(title = "Button group multi-select") { ButtonGroupMultiSelectPreview() }
        ShowcaseSection(title = "Button group multi-select disabled") { ButtonGroupMultiSelectDisabledPreview() }
        ShowcaseSection(title = "Checkbox") { CheckboxPreview() }
        ShowcaseSection(title = "Radio button") { RadioButtonPreview() }
        ShowcaseSection(title = "Switch") { SwitchPreview() }
        ShowcaseSection(title = "Dropdown") { DropdownPreview() }
        ShowcaseSection(title = "Chip Assist") { AssistChipPreview() }
        ShowcaseSection(title = "Chip Filter") { FilterChipPreview() }
        ShowcaseSection(title = "Chip Input") { InputChipPreview() }
        ShowcaseSection(title = "Chip Suggestion") { SuggestionChipPreview() }
    }
}

@Composable
private fun ButtonPreview() {
    Row(horizontalArrangement = Arrangement.spacedBy(space = 24.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(space = 8.dp)) {
            Button(style = ButtonStyle.FILLED, text = "Filled")
            Button(style = ButtonStyle.FILLED_TONAL, text = "Filled tonal")
            Button(style = ButtonStyle.ELEVATED, text = "Elevated")
            Button(style = ButtonStyle.OUTLINED, text = "Outlined")
            Button(style = ButtonStyle.TEXT, text = "Text")
        }
        Column(verticalArrangement = Arrangement.spacedBy(space = 8.dp)) {
            Button(style = ButtonStyle.FILLED, text = "Filled", enabled = false)
            Button(style = ButtonStyle.FILLED_TONAL, text = "Filled tonal", enabled = false)
            Button(style = ButtonStyle.ELEVATED, text = "Elevated", enabled = false)
            Button(style = ButtonStyle.OUTLINED, text = "Outlined", enabled = false)
            Button(style = ButtonStyle.TEXT, text = "Text", enabled = false)
        }
        Column(verticalArrangement = Arrangement.spacedBy(space = 8.dp)) {
            Button(style = ButtonStyle.FILLED, text = "Filled", content = { Icon(imageVector = Icons.Default.Apps) })
            Button(style = ButtonStyle.FILLED_TONAL, text = "Filled tonal", content = { Icon(imageVector = Icons.Default.Apps) })
            Button(style = ButtonStyle.ELEVATED, text = "Elevated", content = { Icon(imageVector = Icons.Default.Apps) })
            Button(style = ButtonStyle.OUTLINED, text = "Outlined", content = { Icon(imageVector = Icons.Default.Apps) })
            Button(style = ButtonStyle.TEXT, text = "Text", content = { Icon(imageVector = Icons.Default.Apps) })
        }
        Column(verticalArrangement = Arrangement.spacedBy(space = 8.dp)) {
            Button(style = ButtonStyle.FILLED, text = "Filled", enabled = false, content = { Icon(imageVector = Icons.Default.Apps) })
            Button(style = ButtonStyle.FILLED_TONAL, text = "Filled tonal", enabled = false, content = { Icon(imageVector = Icons.Default.Apps) })
            Button(style = ButtonStyle.ELEVATED, text = "Elevated", enabled = false, content = { Icon(imageVector = Icons.Default.Apps) })
            Button(style = ButtonStyle.OUTLINED, text = "Outlined", enabled = false, content = { Icon(imageVector = Icons.Default.Apps) })
            Button(style = ButtonStyle.TEXT, text = "Text", enabled = false, content = { Icon(imageVector = Icons.Default.Apps) })
        }
        Column(verticalArrangement = Arrangement.spacedBy(space = 8.dp)) {
            Button()
            Button(loading = true, text = "Button")
            Button(loading = true, enabled = false, text = "Button")
            var loading by remember { mutableStateOf(value = false) }
            LaunchedEffect(key1 = loading) {
                if (loading) {
                    delay(timeMillis = 2000)
                    loading = false
                }
            }
            Button(loading = loading, enabled = true, text = "Click me!", onClick = { loading = true })
        }
    }
}

@Composable
private fun IconButtonPreview() {
    Row(horizontalArrangement = Arrangement.spacedBy(space = 24.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(space = 8.dp)) {
            ButtonStyle.entries.forEach { style ->
                Button(style = style, content = { Icon(imageVector = Icons.Default.Apps) })
            }
        }
        Column(verticalArrangement = Arrangement.spacedBy(space = 8.dp)) {
            ButtonStyle.entries.forEach { style ->
                Button(style = style, enabled = false, content = { Icon(imageVector = Icons.Default.Apps) })
            }
        }
        Column(verticalArrangement = Arrangement.spacedBy(space = 8.dp)) {
            Button(loading = true, content = { Icon(imageVector = Icons.Default.Apps) })
            Button(loading = true, enabled = false, content = { Icon(imageVector = Icons.Default.Apps) })
            var loading by remember { mutableStateOf(value = false) }
            LaunchedEffect(key1 = loading) {
                if (loading) {
                    delay(timeMillis = 2000)
                    loading = false
                }
            }
            Button(loading = loading, enabled = true, onClick = { loading = true }, content = { Icon(imageVector = Icons.Default.Apps) })
        }
    }
}

@Composable
private fun ToggleButtonPreview() {
    Row(horizontalArrangement = Arrangement.spacedBy(space = 24.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(space = 8.dp)) {
            ButtonStyle.entries.forEach { style ->
                var checked by remember { mutableStateOf(value = true) }
                Button(style = style, checked = checked, onClick = { checked = it }, content = { Icon(imageVector = Icons.Default.Apps) })
            }
        }
        Column(verticalArrangement = Arrangement.spacedBy(space = 8.dp)) {
            ButtonStyle.entries.forEach { style ->
                Button(style = style, checked = true, enabled = false, content = { Icon(imageVector = Icons.Default.Apps) })
            }
        }
        Column(verticalArrangement = Arrangement.spacedBy(space = 8.dp)) {
            ButtonStyle.entries.forEach { style ->
                Button(style = style, checked = false, enabled = false, content = { Icon(imageVector = Icons.Default.Apps) })
            }
        }
        Column(verticalArrangement = Arrangement.spacedBy(space = 8.dp)) {
            Button(loading = true, checked = true, content = { Icon(imageVector = Icons.Default.Apps) })
            Button(loading = true, checked = true, enabled = false, content = { Icon(imageVector = Icons.Default.Apps) })
            var loading by remember { mutableStateOf(value = false) }
            LaunchedEffect(key1 = loading) {
                if (loading) {
                    delay(timeMillis = 2000)
                    loading = false
                }
            }
            Button(loading = loading, checked = true, enabled = true, onClick = { loading = true }, content = { Icon(imageVector = Icons.Default.Apps) })
        }
    }
}

@Composable
private fun FabPreview() {
    Column(verticalArrangement = Arrangement.spacedBy(space = 8.dp)) {
        Fab(size = FabSize.SMALL) { Icon(imageVector = Icons.Default.Add) }
        Fab(size = FabSize.REGULAR) { Icon(imageVector = Icons.Default.Add) }
        Fab(size = FabSize.LARGE) { Icon(imageVector = Icons.Default.Add) }
    }
}

private val items = listOf(
    "Bold" to Icons.Default.FormatBold,
    "Italic" to Icons.Default.FormatItalic,
    "Underline" to Icons.Default.FormatUnderlined
)

@Composable
private fun ButtonGroupPreview() {
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

@Composable
private fun ButtonGroupDisabledPreview() {
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

@Composable
private fun ButtonGroupConnectedPreview() {
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

@Composable
private fun ButtonGroupConnectedDisabledPreview() {
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

@Composable
private fun ButtonGroupMultiSelectPreview() {
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

@Composable
private fun ButtonGroupMultiSelectDisabledPreview() {
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

@Composable
private fun CheckboxPreview() {
    Column(verticalArrangement = Arrangement.spacedBy(space = 8.dp)) {
        var checked by remember { mutableStateOf(value = true) }
        Checkbox(checked = checked, onCheckedChange = { checked = it })
        Checkbox(checked = checked, enabled = false, onCheckedChange = { checked = it })
    }
}

@Composable
private fun RadioButtonPreview() {
    Column(verticalArrangement = Arrangement.spacedBy(space = 8.dp)) {
        var selectedIndex by remember { mutableIntStateOf(value = 0) }
        (0..2).forEach { index ->
            RadioButton(selected = index == selectedIndex, onClick = { selectedIndex = index })
        }
        RadioButton(selected = true, enabled = false, onClick = {})
        RadioButton(selected = false, enabled = false, onClick = {})
    }
}

@Composable
private fun SwitchPreview() {
    Column(verticalArrangement = Arrangement.spacedBy(space = 8.dp)) {
        var checked by remember { mutableStateOf(value = true) }
        Switch(checked = checked, onCheckedChange = { checked = it })
        Switch(checked = checked, enabled = false, onCheckedChange = { checked = it })
    }
}

@Composable
private fun DropdownPreview() {
    var expanded by remember { mutableStateOf(value = false) }
    var selected by remember { mutableStateOf(value = "Item 1") }
    Box {
        Button(text = selected, onClick = { expanded = true })
        Dropdown(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            items = persistentListOf(
                DropdownItem(
                    text = "Item 1",
                    leadingIcon = { Icon(imageVector = Icons.Default.Apps) },
                    onClick = {
                        selected = "Item 1"
                        expanded = false
                    }
                ),
                DropdownItem(
                    text = "Item 2",
                    leadingIcon = { Icon(imageVector = Icons.Default.Apps) },
                    onClick = {
                        selected = "Item 2"
                        expanded = false
                    }
                ),
                DropdownItem(text = "Item 3 (disabled)", enabled = false),
            ),
        )
    }
}

@Composable
private fun AssistChipPreview() {
    var count by remember { mutableIntStateOf(value = 0) }
    Row(horizontalArrangement = Arrangement.spacedBy(space = 12.dp)) {
        Chip(
            variant = ChipVariant.ASSIST,
            text = "Add item ($count)",
            leadingIcon = { Icon(imageVector = Icons.Default.Add) },
            onClick = { count++ }
        )
    }
}

@Composable
private fun FilterChipPreview() {
    val labels = listOf("Small", "Medium", "Large")
    var selected by remember { mutableStateOf(value = setOf(labels[0])) }
    Row(horizontalArrangement = Arrangement.spacedBy(space = 12.dp)) {
        labels.forEach { label ->
            Chip(
                variant = ChipVariant.FILTER,
                text = label,
                selected = label in selected,
                leadingIcon = if (label in selected) {
                    { Icon(imageVector = Icons.Default.Check) }
                } else null,
                onClick = {
                    selected = if (label in selected) selected - label else selected + label
                }
            )
        }
    }
}

@Composable
private fun InputChipPreview() {
    val defaults = listOf("Design", "Kotlin", "Compose")
    var items by remember { mutableStateOf(value = defaults) }
    Column(verticalArrangement = Arrangement.spacedBy(space = 8.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(space = 12.dp)) {
            items.forEach { label ->
                Chip(
                    variant = ChipVariant.INPUT,
                    text = label,
                    selected = true,
                    trailingIcon = { Icon(imageVector = Icons.Default.Close) },
                    onClick = { items = items - label }
                )
            }
        }
        Button(text = "Reset") {
            items = defaults
        }
    }
}

@Composable
private fun SuggestionChipPreview() {
    Row(horizontalArrangement = Arrangement.spacedBy(space = 12.dp)) {
        Chip(variant = ChipVariant.SUGGESTION, text = "Try this")
        Chip(variant = ChipVariant.SUGGESTION, text = "Or this")
    }
}
