package com.app.builder.ui.component.card

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.app.builder.ui.LocalTypography
import com.app.builder.ui.Preview
import com.app.builder.ui.core.button.Switch
import com.app.builder.ui.core.card.Card
import com.app.builder.ui.core.progress.ProgressIndicator
import com.app.builder.ui.core.text.Input
import com.app.builder.ui.core.text.Text

/**
 * A card displaying a config entry and its editable [value].
 *
 * @param modifier The [Modifier] to be applied to the root layout.
 * @param name The name of the config entry.
 * @param description A human-readable explanation of what the entry controls.
 * @param value The current value.
 * @param pending Whether an edit to this entry is currently being persisted. Highlights the card and shows a small progress indicator while `true`.
 * @param onValueChange Callback invoked with the updated value when the user edits it.
 */
@Composable
fun ConfigCardSwitch(
    modifier: Modifier = Modifier,
    name: String = "",
    description: String = "",
    value: Boolean = false,
    pending: Boolean = false,
    onValueChange: (Boolean) -> Unit = {},
) {
    val typography = LocalTypography.current

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(all = 4.dp),
        selected = pending,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(all = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(space = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                modifier = Modifier.weight(weight = 1f),
                verticalArrangement = Arrangement.spacedBy(space = 4.dp, alignment = Alignment.Top),
                horizontalAlignment = Alignment.Start,
            ) {
                Text(text = name, translate = false, style = typography.titleMedium)
                Text(text = description, translate = false, style = typography.bodyMedium)
            }
            AnimatedVisibility(
                visible = pending,
                enter = fadeIn(),
                exit = fadeOut(),
            ) {
                ProgressIndicator(modifier = Modifier.size(size = 16.dp))
            }
            Switch(
                checked = value,
                onCheckedChange = { onValueChange(it) }
            )
        }
    }
}

/**
 * A card displaying a config entry and its editable [value].
 *
 * @param modifier The [Modifier] to be applied to the root layout.
 * @param name The name of the config entry.
 * @param description A human-readable explanation of what the entry controls.
 * @param value The current value.
 * @param pending Whether an edit to this entry is currently being persisted. Highlights the card and shows a small progress indicator while `true`.
 * @param onValueChange Callback invoked with the updated value when the user edits it.
 */
@Composable
fun ConfigCardInput(
    modifier: Modifier = Modifier,
    name: String,
    description: String,
    value: String,
    pending: Boolean = false,
    onValueChange: (String) -> Unit = {},
) {
    val typography = LocalTypography.current

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(all = 4.dp),
        selected = pending,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(all = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(space = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                modifier = Modifier.weight(weight = 1f),
                verticalArrangement = Arrangement.spacedBy(space = 4.dp, alignment = Alignment.Top),
                horizontalAlignment = Alignment.Start,
            ) {
                Text(text = name, translate = false, style = typography.titleMedium)
                Text(text = description, translate = false, style = typography.bodyMedium)
            }
            AnimatedVisibility(
                visible = pending,
                enter = fadeIn(),
                exit = fadeOut(),
            ) {
                ProgressIndicator(modifier = Modifier.size(size = 16.dp))
            }
            Input(
                modifier = Modifier.width(width = 160.dp),
                maxLines = 1,
                value = value,
                onValueChange = { onValueChange(it) }
            )
        }
    }
}

@Preview
@Composable
private fun ConfigCardSwitchPreview() = Preview {
    ConfigCardSwitch(
        name = "Awesome flag",
        description = "Toggles being awesome.",
        value = true
    )
}

@Preview
@Composable
private fun ConfigCardInputPreview() = Preview {
    ConfigCardInput(
        name = "Timeout",
        description = "Maximum time in milliseconds allowed to be even more awesome.",
        value = "987654321"
    )
}

@Preview
@Composable
private fun ConfigCardLoadingPreview() = Preview {
    ConfigCardSwitch(
        name = "Loading",
        description = "",
        value = true,
        pending = true
    )
}
