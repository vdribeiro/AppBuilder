package com.app.builder.ui.component.card

import kotlinx.collections.immutable.persistentListOf
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.app.builder.ui.InjectTranslations
import com.app.builder.ui.LocalTypography
import com.app.builder.ui.Preview
import com.app.builder.ui.component.field.DateTimeField
import com.app.builder.ui.component.field.FieldValue
import com.app.builder.ui.component.field.MetadataSection
import com.app.builder.ui.core.button.Chip
import com.app.builder.ui.core.card.Card
import com.app.builder.ui.core.text.Text
import com.app.builder.ui.getDateTime
import com.app.builder.ui.getTranslation

/**
 * A time punch card, showing only the properties that are not `null`.
 *
 * @param modifier The [Modifier] to be applied to the root layout.
 * @param enabled Whether the time punch's properties can be edited.
 * @param uuid The UUID of the time punch.
 * @param modifiedAt The modification timestamp of the time punch, as a UTC ISO-8601 string shown as a localized date-time.
 * @param deletedAt The deletion timestamp of the time punch, as a UTC ISO-8601 string shown as a localized date-time.
 * @param userUuid The UUID of the user that punched the clock.
 * @param userName The name of the user that punched the clock.
 * @param onUserClick Callback for when the user is clicked while [enabled], or `null` when the user cannot be changed.
 * @param time The time at which the user punched the clock, as a UTC ISO-8601 string shown as a localized date-time.
 * @param onTimeChange Callback for when the time changes, with the new time as a UTC ISO-8601 string.
 * @param source The time punch's source as a translation key.
 */
@Composable
fun TimePunchCard(
    modifier: Modifier = Modifier,
    enabled: Boolean = false,
    uuid: String? = null,
    modifiedAt: String? = null,
    deletedAt: String? = null,
    userUuid: String? = null,
    userName: String? = null,
    onUserClick: (() -> Unit)? = null,
    time: String? = null,
    onTimeChange: (String) -> Unit = {},
    source: String? = null,
) {
    val typography = LocalTypography.current

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(all = 8.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
        ) {
            if (enabled && onUserClick != null) Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(space = 4.dp),
            ) {
                Text(
                    text = getTranslation(key = "time_punch_user_name"),
                    maxLines = 1,
                    style = typography.labelMedium
                )
                Chip(
                    modifier = Modifier.testTag(tag = "time_punch_user_pick"),
                    text = userName,
                    onClick = onUserClick
                )
            } else userName?.let {
                FieldValue(
                    modifier = Modifier.testTag(tag = "time_punch_user_name"),
                    label = "time_punch_user_name",
                    value = it
                )
            }
            time?.let {
                DateTimeField(
                    modifier = Modifier.testTag(tag = "time_punch_time"),
                    label = "time_punch_time",
                    value = it,
                    editing = enabled,
                    confirmLabel = "time_punch_picker_apply",
                    dismissLabel = "time_punch_picker_cancel",
                    onValueChange = onTimeChange
                )
            }
            source?.let {
                FieldValue(
                    modifier = Modifier.testTag(tag = "time_punch_source"),
                    label = "time_punch_source",
                    value = getTranslation(key = it)
                )
            }
            MetadataSection(
                entries = persistentListOf(
                    "time_punch_uuid" to uuid,
                    "time_punch_user_uuid" to userUuid,
                    "time_punch_modified_at" to modifiedAt?.let { getDateTime(utc = it) },
                    "time_punch_deleted_at" to deletedAt?.let { getDateTime(utc = it) },
                )
            )
        }
    }
}

@PreviewLightDark
@Composable
private fun TimePunchCardPreview() = Preview {
    InjectTranslations(
        translations = mapOf(
            "time_punch_user_name" to "User Name",
            "time_punch_time" to "Time",
            "time_punch_source" to "Source",
            "time_punch_source_manual" to "Manual",
            "time_punch_modified_at" to "Modified At",
        )
    )
    TimePunchCard(
        modifiedAt = "2026-11-09T10:00:00Z",
        userName = "Vitor Ribeiro",
        time = "2026-11-09T09:00:00Z",
        source = "time_punch_source_manual"
    )
}
