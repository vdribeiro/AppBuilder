package com.app.builder.ui.core.button

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.app.builder.ui.Preview
import androidx.compose.material3.Switch as MaterialSwitch

/**
 * Switch component that provides a standardized toggle switch for binary state selections.
 *
 * @param modifier The [Modifier] to be applied to the layout of the switch.
 * @param enabled Controls the enabled state of the switch. When `false`, user interactions are disabled.
 * @param checked Whether this switch is checked (turned on) or unchecked (turned off).
 * @param onCheckedChange Callback invoked when the user toggles the switch state. If null, the switch becomes read-only but remains enabled visually depending on the [enabled] flag.
 */
@Composable
fun Switch(
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    checked: Boolean = false,
    onCheckedChange: ((Boolean) -> Unit)? = null,
) {
    MaterialSwitch(
        modifier = modifier,
        enabled = enabled,
        checked = checked,
        onCheckedChange = onCheckedChange
    )
}

@Preview
@Composable
private fun SwitchPreview() = Preview {
    Column(verticalArrangement = Arrangement.spacedBy(space = 8.dp)) {
        var checked by remember { mutableStateOf(value = true) }
        Switch(checked = checked, onCheckedChange = { checked = it })
        Switch(checked = checked, enabled = false)
    }
}
