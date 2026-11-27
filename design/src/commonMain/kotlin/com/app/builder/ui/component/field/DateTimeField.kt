package com.app.builder.ui.component.field

import kotlin.time.Instant
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.app.builder.core.locale.toInstant
import com.app.builder.ui.DateTimeMode
import com.app.builder.ui.LocalTypography
import com.app.builder.ui.Preview
import com.app.builder.ui.core.image.Icon
import com.app.builder.ui.core.text.Text
import com.app.builder.ui.getDateTime
import com.app.builder.ui.getTranslation

/**
 * A property holding a moment in time, of which [mode] shows and changes the date and the time of day, only the date, or only the time of day.
 * While [editing], clicking it opens the picker of its [mode] in a dialog, and the value changes once it is confirmed.
 * Otherwise, [value] shows as a [FieldValue].
 *
 * @param modifier The [Modifier] to be applied to the root layout.
 * @param label The label of the field, translated.
 * @param mode The parts of [value] that are shown and changed.
 * @param value The moment as a UTC ISO-8601 string, shown as a localized string, or `null` when there is none.
 * @param editing Whether the value can be changed.
 * @param confirmLabel The label of the confirm button of the pickers, translated.
 * @param dismissLabel The label of the dismiss button of the pickers, translated.
 * @param onValueChange Callback for when the value changes, with the new moment as a UTC ISO-8601 string.
 */
@Composable
fun DateTimeField(
    modifier: Modifier = Modifier,
    label: String = "",
    mode: DateTimeMode = DateTimeMode.DATE_TIME,
    value: String? = null,
    editing: Boolean = false,
    confirmLabel: String = "",
    dismissLabel: String = "",
    onValueChange: (String) -> Unit = {},
) {
    val typography = LocalTypography.current
    val text = value?.let { getDateTime(utc = it, mode = mode) }

    if (!editing) {
        FieldValue(modifier = modifier, label = label, value = text)
        return
    }

    var pickerOpen by remember { mutableStateOf(value = false) }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable { pickerOpen = true }
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(space = 8.dp),
    ) {
        Column(
            modifier = Modifier.weight(weight = 1f),
            verticalArrangement = Arrangement.spacedBy(space = 2.dp),
        ) {
            Text(
                text = getTranslation(key = label),
                maxLines = 1,
                style = typography.labelMedium
            )
            Text(
                text = text?.takeIf { it.isNotBlank() } ?: EMPTY_VALUE,
                maxLines = 1,
                style = typography.bodyLarge
            )
        }
        Icon(imageVector = if (mode == DateTimeMode.TIME) Icons.Default.Schedule else Icons.Default.CalendarToday)
    }

    if (pickerOpen) {
        val selected = value?.toInstant()
        val confirmText = getTranslation(key = confirmLabel)
        val dismissText = getTranslation(key = dismissLabel)
        val onSelected: (Instant) -> Unit = {
            pickerOpen = false
            onValueChange(it.toString())
        }
        // TODO
    }
}

/** Shown in place of a missing value. */
private const val EMPTY_VALUE = "—"

@PreviewLightDark
@Composable
private fun DateTimeFieldPreview() = Preview {
    Column(verticalArrangement = Arrangement.spacedBy(space = 8.dp)) {
        DateTimeField(label = "Time", value = "2026-11-09T09:00:00Z")
        DateTimeField(label = "Time", value = "2026-11-09T09:00:00Z", editing = true)
        DateTimeField(label = "Date", mode = DateTimeMode.DATE, value = "2026-11-09T09:00:00Z", editing = true)
        DateTimeField(label = "Hour", mode = DateTimeMode.TIME, value = "2026-11-09T09:00:00Z", editing = true)
        DateTimeField(label = "Hour", mode = DateTimeMode.TIME, editing = true)
    }
}
