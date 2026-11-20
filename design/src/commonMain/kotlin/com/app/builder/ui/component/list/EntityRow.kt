package com.app.builder.ui.component.list

import androidx.compose.foundation.layout.Column
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.tooling.preview.PreviewLightDark
import com.app.builder.ui.Preview
import com.app.builder.ui.core.button.Checkbox
import com.app.builder.ui.core.image.Icon
import com.app.builder.ui.core.list.ListItem
import com.app.builder.ui.getDateTime

/**
 * A compact list row with a leading visual, its headline and supporting text and optional bookkeeping in a small line above the headline.
 * A selected row is tinted, a deleted row is faded with a trash icon at its end, and a checkable row shows a checkbox at its end in place of the trash icon.
 *
 * @param modifier The [Modifier] to be applied to the row's layout.
 * @param selected Whether the row is selected.
 * @param deleted Whether the deleted icon is shown.
 * @param headline The primary text, shown as given.
 * @param supporting The secondary text, shown as given.
 * @param metadata Bookkeeping entries joined into the line above the headline, which is hidden when all their texts are `null`.
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
    metadata: List<EntityRowMetadata> = emptyList(),
    leadingContent: (@Composable () -> Unit)? = null,
    checked: Boolean? = null,
) {
    val overline = metadata.mapNotNull { entry ->
        entry.text?.let { if (entry.dateTime) getDateTime(utc = it) else it }
    }

    ListItem(
        modifier = modifier.alpha(alpha = if (deleted) DELETED_ALPHA else 1f),
        selected = selected,
        translate = false,
        overlineText = overline.takeIf { it.isNotEmpty() }?.joinToString(separator = " · "),
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

/**
 * A bookkeeping entry of an [EntityRow].
 *
 * @property text The text of the entry, or `null` to leave it out.
 * @property dateTime True to show [text], a UTC ISO-8601 string, as a localized date-time.
 */
@Stable
data class EntityRowMetadata(
    val text: String?,
    val dateTime: Boolean = false,
)

/** The opacity of a deleted row. */
private const val DELETED_ALPHA = 0.6f

@PreviewLightDark
@Composable
private fun EntityRowPreview() = Preview {
    Column {
        EntityRow(headline = "Headline", supporting = "Supporting text", leadingContent = { Icon(imageVector = Icons.Default.Apps) })
        EntityRow(selected = true, headline = "Selected", supporting = "Supporting text", leadingContent = { Icon(imageVector = Icons.Default.Apps) })
        EntityRow(deleted = true, headline = "Deleted", metadata = listOf(EntityRowMetadata(text = "6f1c2a8e-0b7d-4c2e"), EntityRowMetadata(text = "2026-11-09T10:00:00Z", dateTime = true)))
    }
}
