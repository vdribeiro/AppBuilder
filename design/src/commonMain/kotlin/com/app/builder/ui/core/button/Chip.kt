package com.app.builder.ui.core.button

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AssistChip
import androidx.compose.material3.FilterChip
import androidx.compose.material3.InputChip
import androidx.compose.material3.SuggestionChip
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.app.builder.ui.LocalTypography
import com.app.builder.ui.Preview
import com.app.builder.ui.core.image.Icon
import com.app.builder.ui.core.text.Text

/** Distinguishes between the visual and interaction treatments a [Chip] can adopt. */
enum class ChipVariant {
    /** Represents a prompt that initiates an action. */
    ASSIST,
    /** Uses tags or descriptive words to filter content, toggled through [Chip]'s `selected` state. */
    FILTER,
    /** Represents discrete pieces of information the user entered, typically paired with a trailing remove affordance. */
    INPUT,
    /** Presents dynamically generated suggestions. */
    SUGGESTION
}

/**
 * Compact element that unifies the assist, filter, input, and suggestion chip styles.
 *
 * @param modifier The [Modifier] to be applied to the chip's layout.
 * @param variant The visual and interaction treatment to render. Defaults to [ChipVariant.ASSIST].
 * @param enabled Controls the enabled state of the chip. When `false`, the chip is not clickable.
 * @param selected Whether the chip is currently toggled on. Only meaningful for [ChipVariant.FILTER] and [ChipVariant.INPUT].
 * @param text The text label to display inside the chip.
 * @param leadingIcon An optional composable slot rendering a leading icon or avatar.
 * @param trailingIcon An optional composable slot rendering a trailing icon, such as a remove affordance. Ignored for [ChipVariant.SUGGESTION].
 * @param onClick Called when the user clicks the chip.
 */
@Composable
fun Chip(
    modifier: Modifier = Modifier,
    variant: ChipVariant = ChipVariant.ASSIST,
    enabled: Boolean = true,
    selected: Boolean = false,
    text: String? = null,
    leadingIcon: (@Composable () -> Unit)? = null,
    trailingIcon: (@Composable () -> Unit)? = null,
    onClick: () -> Unit = {},
) {
    val typography = LocalTypography.current
    val label: @Composable () -> Unit = {
        Text(
            text = text.orEmpty(),
            maxLines = 1,
            style = typography.labelLarge
        )
    }

    when (variant) {
        ChipVariant.ASSIST -> AssistChip(
            modifier = modifier,
            onClick = onClick,
            enabled = enabled,
            label = label,
            leadingIcon = leadingIcon,
            trailingIcon = trailingIcon
        )

        ChipVariant.FILTER -> FilterChip(
            modifier = modifier,
            selected = selected,
            onClick = onClick,
            enabled = enabled,
            label = label,
            leadingIcon = leadingIcon,
            trailingIcon = trailingIcon
        )

        ChipVariant.INPUT -> InputChip(
            modifier = modifier,
            selected = selected,
            onClick = onClick,
            enabled = enabled,
            label = label,
            leadingIcon = leadingIcon,
            trailingIcon = trailingIcon
        )

        ChipVariant.SUGGESTION -> SuggestionChip(
            modifier = modifier,
            onClick = onClick,
            enabled = enabled,
            label = label,
            icon = leadingIcon
        )
    }
}

@Preview
@Composable
private fun AssistChipPreview() = Preview {
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

@Preview
@Composable
private fun FilterChipPreview() = Preview {
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

@Preview
@Composable
private fun InputChipPreview() = Preview {
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

@Preview
@Composable
private fun SuggestionChipPreview() = Preview {
    Row(horizontalArrangement = Arrangement.spacedBy(space = 12.dp)) {
        Chip(variant = ChipVariant.SUGGESTION, text = "Try this")
        Chip(variant = ChipVariant.SUGGESTION, text = "Or this")
    }
}
