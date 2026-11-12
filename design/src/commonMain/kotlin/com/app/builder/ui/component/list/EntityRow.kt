package com.app.builder.ui.component.list

import androidx.compose.foundation.layout.Column
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.tooling.preview.PreviewLightDark
import com.app.builder.ui.Preview
import com.app.builder.ui.core.button.Checkbox
import com.app.builder.ui.core.image.Icon
import com.app.builder.ui.core.list.ListItem

/**
 * A compact list row with a leading visual, its headline and supporting text and optional bookkeeping in a small line above the headline.
 * A selected row is tinted, a deleted row is faded with a trash icon at its end, and a checkable row shows a checkbox at its end in place of the trash icon.
 *
 * @param modifier The [Modifier] to be applied to the row's layout.
 * @param selected Whether the row is selected.
 * @param deleted Whether the deleted icon is shown.
 * @param headline The primary text, shown as given.
 * @param supporting The secondary text, shown as given.
 * @param metadata Bookkeeping texts joined into the line above the headline, which is hidden when all are `null`.
 * @param leadingContent An optional composable slot rendered at the start of the row, typically an icon or avatar.
 * @param checked Whether the row's checkbox is ticked, or `null` for a row without a checkbox.
 */
@Composable
fun EntityRow(
    modifier: Modifier = Modifier,
    selected: Boolean = false,
    deleted: Boolean = false,
    headline: String? = null,
    supporting: String? = null,
    metadata: List<String?> = emptyList(),
    leadingContent: (@Composable () -> Unit)? = null,
    checked: Boolean? = null,
) {
    ListItem(
        modifier = modifier.alpha(alpha = if (deleted) DELETED_ALPHA else 1f),
        selected = selected,
        translate = false,
        overlineText = metadata.filterNotNull().takeIf { it.isNotEmpty() }?.joinToString(separator = " · "),
        headlineText = headline,
        supportingText = supporting?.takeIf { it.isNotBlank() },
        leadingContent = leadingContent,
        trailingContent = when {
            checked != null -> {
                { Checkbox(checked = checked) }
            }
            deleted -> {
                { Icon(imageVector = Icons.Outlined.Delete, contentDescription = "deleted") }
            }
            else -> null
        }
    )
}

/** The opacity of a deleted row. */
private const val DELETED_ALPHA = 0.6f

@PreviewLightDark
@Composable
private fun EntityRowPreview() = Preview {
    Column {
        EntityRow(headline = "Headline", supporting = "Supporting text", leadingContent = { Icon(imageVector = Icons.Default.Apps) })
        EntityRow(selected = true, headline = "Selected", supporting = "Supporting text", leadingContent = { Icon(imageVector = Icons.Default.Apps) })
        EntityRow(deleted = true, headline = "Deleted", metadata = listOf("6f1c2a8e-0b7d-4c2e", "2026-11-09T10:00:00Z"))
    }
}
