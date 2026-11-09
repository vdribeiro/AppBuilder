package com.app.builder.ui.component.field

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.toggleable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.app.builder.ui.LocalTypography
import com.app.builder.ui.Preview
import com.app.builder.ui.core.button.Switch
import com.app.builder.ui.core.text.Text

/**
 * An on/off property: its label beside a switch. The whole row toggles the value, and only while [editing].
 *
 * @param modifier The [Modifier] to be applied to the root layout, which carries the toggle semantics.
 * @param label The label of the field, translated.
 * @param checked Whether the value is on.
 * @param editing Whether the value can be changed.
 * @param onCheckedChange Callback for when the value is toggled.
 */
@Composable
fun BooleanField(
    modifier: Modifier = Modifier,
    label: String = "",
    checked: Boolean = false,
    editing: Boolean = false,
    onCheckedChange: (Boolean) -> Unit = {},
) {
    val typography = LocalTypography.current

    Row(
        modifier = modifier
            .fillMaxWidth()
            .toggleable(value = checked, enabled = editing, role = Role.Switch, onValueChange = onCheckedChange)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            modifier = Modifier.weight(weight = 1f),
            text = label,
            maxLines = 1,
            style = typography.bodyLarge
        )
        Switch(enabled = editing, checked = checked)
    }
}

@PreviewLightDark
@Composable
private fun BooleanFieldPreview() = Preview {
    var checked by remember { mutableStateOf(value = true) }
    Column(verticalArrangement = Arrangement.spacedBy(space = 8.dp)) {
        BooleanField(label = "Active", checked = checked)
        BooleanField(label = "Active", checked = checked, editing = true, onCheckedChange = { checked = it })
    }
}
