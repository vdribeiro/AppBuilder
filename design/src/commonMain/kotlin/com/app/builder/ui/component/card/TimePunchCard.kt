package com.app.builder.ui.component.card

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.app.builder.ui.InjectTranslations
import com.app.builder.ui.Preview
import com.app.builder.ui.component.field.FieldValue
import com.app.builder.ui.component.field.MetadataSection
import com.app.builder.ui.core.card.Card
import com.app.builder.ui.getDateTime
import com.app.builder.ui.getTranslation
import kotlinx.collections.immutable.persistentListOf

/**
 * A time punch card, showing only the properties that are not `null`.
 *
 * @param modifier The [Modifier] to be applied to the root layout.
 * @param uuid The UUID of the time punch.
 * @param modifiedAt The modification timestamp of the time punch, as a UTC ISO-8601 string shown as a localized date-time.
 * @param deletedAt The deletion timestamp of the time punch, as a UTC ISO-8601 string shown as a localized date-time.
 * @param userName The name of the user that punched the clock.
 * @param time The time at which the user punched the clock, as a UTC ISO-8601 string shown as a localized date-time.
 * @param source The time punch's source as a translation key.
 */
@Composable
fun TimePunchCard(
    modifier: Modifier = Modifier,
    uuid: String? = null,
    modifiedAt: String? = null,
    deletedAt: String? = null,
    userName: String? = null,
    time: String? = null,
    source: String? = null,
) {
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
            userName?.let {
                FieldValue(
                    modifier = Modifier.testTag(tag = "time_punch_user"),
                    label = "time_punch_user",
                    value = it
                )
            }
            time?.let {
                FieldValue(
                    modifier = Modifier.testTag(tag = "time_punch_time"),
                    label = "time_punch_time",
                    value = getDateTime(utc = it)
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
            "time_punch_user" to "User",
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
