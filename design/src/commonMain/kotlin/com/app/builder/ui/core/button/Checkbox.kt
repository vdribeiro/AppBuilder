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
import androidx.compose.material3.Checkbox as MaterialCheckbox

/**
 * Checkbox component that lets users select one or more items from a set, or mark a single item as on or off.
 *
 * @param modifier The [Modifier] to be applied to the layout of the checkbox.
 * @param enabled Controls the enabled state of the checkbox. When `false`, user interactions are disabled.
 * @param checked Whether this checkbox is checked or unchecked.
 * @param onCheckedChange Callback invoked when the user toggles the checkbox state. If null, the checkbox becomes read-only but remains visually enabled depending on the [enabled] flag.
 */
@Composable
fun Checkbox(
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    checked: Boolean = false,
    onCheckedChange: ((Boolean) -> Unit)? = null,
) {
    MaterialCheckbox(
        modifier = modifier,
        enabled = enabled,
        checked = checked,
        onCheckedChange = onCheckedChange
    )
}

@Preview
@Composable
private fun CheckboxPreview() = Preview {
    Column(verticalArrangement = Arrangement.spacedBy(space = 8.dp)) {
        var checked by remember { mutableStateOf(value = true) }
        Checkbox(checked = checked, onCheckedChange = { checked = it })
        Checkbox(checked = checked, enabled = false)
    }
}
