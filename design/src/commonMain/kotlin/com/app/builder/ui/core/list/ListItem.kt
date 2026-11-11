package com.app.builder.ui.core.list

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material3.ListItemDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.app.builder.ui.LocalColorScheme
import com.app.builder.ui.LocalTypography
import com.app.builder.ui.Preview
import com.app.builder.ui.core.image.Icon
import com.app.builder.ui.core.text.Text
import androidx.compose.material3.ListItem as MaterialListItem

/**
 * A single row entry for lists and menus, standardizing headline, supporting text, and leading/trailing decoration slots.
 *
 * @param modifier The [Modifier] to be applied to the row's layout.
 * @param selected When `true`, the row is drawn on the secondary container color.
 * @param translate True to translate the texts, false to show them as given. Defaults to true.
 * @param overlineText An optional small text rendered above the headline.
 * @param headlineText The primary text label for the row.
 * @param supportingText An optional secondary text description rendered below the headline.
 * @param leadingContent An optional composable slot rendered at the start of the row, typically an [Icon].
 * @param trailingContent An optional composable slot rendered at the end of the row, typically an [Icon] or [Text].
 */
@Composable
fun ListItem(
    modifier: Modifier = Modifier,
    selected: Boolean = false,
    translate: Boolean = true,
    overlineText: String? = null,
    headlineText: String? = null,
    supportingText: String? = null,
    leadingContent: (@Composable () -> Unit)? = null,
    trailingContent: (@Composable () -> Unit)? = null,
) {
    val typography = LocalTypography.current
    val colorScheme = LocalColorScheme.current

    MaterialListItem(
        modifier = modifier,
        colors = if (selected) ListItemDefaults.colors(
            containerColor = colorScheme.secondaryContainer,
            headlineColor = colorScheme.onSecondaryContainer,
            supportingColor = colorScheme.onSecondaryContainer,
            overlineColor = colorScheme.onSecondaryContainer,
            leadingIconColor = colorScheme.onSecondaryContainer,
            trailingIconColor = colorScheme.onSecondaryContainer,
        ) else ListItemDefaults.colors(),
        overlineContent = overlineText?.let {
            {
                Text(
                    text = it,
                    translate = translate,
                    textAlign = TextAlign.Start,
                    maxLines = 1,
                    style = typography.labelSmall
                )
            }
        },
        headlineContent = {
            headlineText?.let {
                Text(
                    text = it,
                    translate = translate,
                    textAlign = TextAlign.Start,
                    maxLines = 1,
                    style = typography.bodyLarge
                )
            }
        },
        supportingContent = supportingText?.let {
            {
                Text(
                    text = it,
                    translate = translate,
                    textAlign = TextAlign.Start,
                    style = typography.bodyMedium
                )
            }
        },
        leadingContent = leadingContent,
        trailingContent = trailingContent
    )
}

@PreviewLightDark
@Composable
private fun ListItemPreview() = Preview {
    Column(verticalArrangement = Arrangement.spacedBy(space = 8.dp)) {
        ListItem(
            headlineText = "Headline",
            supportingText = "Supporting text",
            leadingContent = { Icon(imageVector = Icons.Default.Apps) },
            trailingContent = { Icon(imageVector = Icons.Default.Apps) }
        )
        ListItem(
            headlineText = "Headline",
            supportingText = "Supporting text",
            leadingContent = { Icon(imageVector = Icons.Default.Apps) },
        )
        ListItem(
            headlineText = "Headline",
            supportingText = "Supporting text",
            trailingContent = { Icon(imageVector = Icons.Default.Apps) }
        )
        ListItem(
            headlineText = "Headline",
            supportingText = "Supporting text",
        )
        ListItem(
            headlineText = "Headline",
        )
        ListItem(
            supportingText = "Supporting text",
            leadingContent = { Icon(imageVector = Icons.Default.Apps) },
        )
    }
}
