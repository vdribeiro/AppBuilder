package com.app.builder.ui.core.list

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.app.builder.ShowcaseSection
import com.app.builder.ui.core.card.Card
import com.app.builder.ui.core.image.Icon
import com.app.builder.ui.core.text.Text
import com.app.builder.ui.core.list.LazyColumn as ScrollableLazyColumn

/** Displays a scrollable list mixing list items and cards. */
@Composable
fun ListShowcase() {
    Column(
        modifier = Modifier.padding(all = 16.dp),
        verticalArrangement = Arrangement.spacedBy(space = 32.dp),
    ) {
        ShowcaseSection(title = "Scrollable List with Scrollbar, List Items and Cards") { ScrollableListPreview() }
    }
}

@Composable
private fun ScrollableListPreview() {
    ScrollableLazyColumn(
        modifier = Modifier.height(height = 300.dp),
        verticalArrangement = Arrangement.spacedBy(space = 8.dp),
    ) {
        item {
            ListItem(
                headlineText = "Headline",
                supportingText = "Supporting text",
                leadingContent = { Icon(imageVector = Icons.Default.Apps) },
                trailingContent = { Icon(imageVector = Icons.Default.Apps) }
            )
        }
        item {
            ListItem(
                headlineText = "Headline",
                supportingText = "Supporting text",
                leadingContent = { Icon(imageVector = Icons.Default.Apps) },
            )
        }
        item {
            Card(
                content = {
                    Text(
                        modifier = Modifier.padding(all = 16.dp),
                        text = "Item"
                    )
                }
            )
        }
        item {
            ListItem(
                headlineText = "Headline",
                supportingText = "Supporting text",
                trailingContent = { Icon(imageVector = Icons.Default.Apps) }
            )
        }
        item {
            Card(
                selected = true,
                content = {
                    Text(
                        modifier = Modifier.padding(all = 16.dp),
                        text = "Item"
                    )
                }
            )
        }
        item {
            ListItem(
                headlineText = "Headline",
                supportingText = "Supporting text",
            )
        }
        item {
            ListItem(
                headlineText = "Headline",
            )
        }
        item {
            ListItem(
                supportingText = "Supporting text",
                leadingContent = { Icon(imageVector = Icons.Default.Apps) },
            )
        }
    }
}
