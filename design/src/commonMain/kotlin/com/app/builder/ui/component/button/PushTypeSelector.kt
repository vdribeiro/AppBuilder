package com.app.builder.ui.component.button

import kotlinx.collections.immutable.toPersistentList
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.app.builder.ui.Preview
import com.app.builder.ui.core.button.Button
import com.app.builder.ui.core.button.Dropdown
import com.app.builder.ui.core.button.DropdownItem
import com.app.builder.ui.core.image.Icon

/** The kind of push being composed. */
enum class PushPayloadType {
    NOTIFICATION,
    FLAGS,
    CONFIGS,
    RESET;
}

/**
 * A dropdown selector for [PushPayloadType].
 *
 * @param modifier The [Modifier] to be applied to the root layout.
 * @param type The current [PushPayloadType].
 * @param onTypeSelected Callback invoked with the selected [PushPayloadType] when the user makes a selection.
 */
@Composable
fun PushTypeSelector(
    modifier: Modifier = Modifier,
    type: PushPayloadType = PushPayloadType.NOTIFICATION,
    onTypeSelected: (PushPayloadType) -> Unit = {},
) {
    var expanded by remember { mutableStateOf(value = false) }

    Column(modifier = modifier) {
        Button(
            modifier = Modifier.defaultMinSize(minWidth = 160.dp),
            text = type.name,
            onClick = { expanded = true },
        )
        Dropdown(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            items = PushPayloadType.entries.map { candidate ->
                DropdownItem(
                    text = candidate.name,
                    onClick = {
                        expanded = false
                        onTypeSelected(candidate)
                    },
                    leadingIcon = { if (candidate == type) Icon(imageVector = Icons.Default.Check) }
                )
            }.toPersistentList()
        )
    }
}

@Preview
@Composable
private fun PushTypeSelectorPreview() = Preview {
    PushTypeSelector(
        type = PushPayloadType.NOTIFICATION,
        onTypeSelected = {},
    )
}