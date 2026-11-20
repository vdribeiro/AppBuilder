package com.app.builder.ui.component.container

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.SyncDisabled
import androidx.compose.material.icons.filled.Warning
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.app.builder.ui.LocalTypography
import com.app.builder.ui.Preview
import com.app.builder.ui.core.card.Card
import com.app.builder.ui.core.divider.Divider
import com.app.builder.ui.core.image.Icon
import com.app.builder.ui.core.list.LazyColumn
import com.app.builder.ui.core.list.ListItem
import com.app.builder.ui.core.text.Text
import com.app.builder.ui.getTranslation

/**
 * The application help, explaining each status icon of the top action bar.
 *
 * @param modifier The [Modifier] to be applied to the root layout.
 */
@Composable
fun Help(modifier: Modifier = Modifier) {
    val typography = LocalTypography.current

    LazyColumn(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(space = 24.dp),
    ) {
        item {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(space = 8.dp),
            ) {
                Text(text = getTranslation(key = "help_status_icons"), style = typography.titleMedium)
                Card {
                    ListItem(
                        modifier = Modifier.testTag(tag = "help_session_expired"),
                        headlineText = getTranslation(key = "help_session_expired"),
                        supportingText = getTranslation(key = "help_session_expired_description"),
                        leadingContent = { Icon(imageVector = Icons.Default.Warning) },
                    )
                    Divider()
                    ListItem(
                        modifier = Modifier.testTag(tag = "help_not_synced"),
                        headlineText = getTranslation(key = "help_not_synced"),
                        supportingText = getTranslation(key = "help_not_synced_description"),
                        leadingContent = { Icon(imageVector = Icons.Default.SyncDisabled) },
                    )
                    Divider()
                    ListItem(
                        modifier = Modifier.testTag(tag = "help_clock_untrusted"),
                        headlineText = getTranslation(key = "help_clock_untrusted"),
                        supportingText = getTranslation(key = "help_clock_untrusted_description"),
                        leadingContent = { Icon(imageVector = Icons.Default.Schedule) },
                    )
                }
            }
        }
    }
}

@PreviewLightDark
@Composable
private fun HelpPreview() = Preview {
    Help()
}
