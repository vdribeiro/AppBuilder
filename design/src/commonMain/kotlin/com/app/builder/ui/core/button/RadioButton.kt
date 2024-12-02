package com.app.builder.ui.core.button

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.app.builder.ui.Preview
import androidx.compose.material3.RadioButton as MaterialRadioButton

/**
 * Radio button component that lets users select a single option from a set of mutually exclusive choices.
 *
 * @param modifier The [Modifier] to be applied to the layout of the radio button.
 * @param enabled Controls the enabled state of the radio button. When `false`, user interactions are disabled.
 * @param selected Whether this radio button is currently the selected option within its group.
 * @param onClick Callback invoked when the user selects this option. If null, the radio button becomes read-only.
 */
@Composable
fun RadioButton(
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    selected: Boolean = false,
    onClick: (() -> Unit)? = null,
) {
    MaterialRadioButton(
        modifier = modifier,
        enabled = enabled,
        selected = selected,
        onClick = onClick
    )
}

@Preview
@Composable
private fun RadioButtonPreview() = Preview {
    Column(verticalArrangement = Arrangement.spacedBy(space = 8.dp)) {
        var selectedIndex by remember { mutableIntStateOf(value = 0) }
        (0..2).forEach { index ->
            RadioButton(selected = index == selectedIndex, onClick = { selectedIndex = index })
        }
        RadioButton(selected = true, enabled = false)
        RadioButton(selected = false, enabled = false)
    }
}
