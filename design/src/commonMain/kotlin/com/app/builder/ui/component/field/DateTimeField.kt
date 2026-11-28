package com.app.builder.ui.component.field

import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atTime
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
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
import com.app.builder.core.locale.DateTimeParts
import com.app.builder.core.locale.now
import com.app.builder.core.locale.toInstant
import com.app.builder.ui.LocalTypography
import com.app.builder.ui.Preview
import com.app.builder.ui.core.image.Icon
import com.app.builder.ui.core.overlay.DatePicker
import com.app.builder.ui.core.overlay.DateTimePicker
import com.app.builder.ui.core.overlay.TimePicker
import com.app.builder.ui.core.text.Text
import com.app.builder.ui.getDateTime
import com.app.builder.ui.getTranslation

/**
 * A property holding a moment in time, showing and changing its date when [date] is true and its time of day when [time] is true.
 * While [editing], clicking it opens the date picker, the time picker, or the date and time picker in a dialog, and the value changes once it is confirmed.
 * Otherwise, [value] shows as a [FieldValue].
 *
 * @param modifier The [Modifier] to be applied to the root layout.
 * @param label The label of the field, translated, or `null` for the label of the parts shown: the date, the time of day, or both.
 * @param date Whether the date of [value] is shown and changed.
 * @param time Whether the time of day of [value] is shown and changed.
 * @param value The moment as a UTC ISO-8601 string, shown as a localized string, or `null` when there is none.
 * @param editing Whether the value can be changed.
 * @param confirmLabel The label of the confirm button of the pickers, translated.
 * @param dismissLabel The label of the dismiss button of the pickers, translated.
 * @param onValueChange Callback for when the value changes, with the new moment as a UTC ISO-8601 string.
 */
@Composable
fun DateTimeField(
    modifier: Modifier = Modifier,
    label: String? = null,
    date: Boolean = true,
    time: Boolean = true,
    value: String? = null,
    editing: Boolean = false,
    confirmLabel: String = "",
    dismissLabel: String = "",
    onValueChange: (String) -> Unit = {},
) {
    val typography = LocalTypography.current
    val labelKey = label ?: when {
        date && time -> "date_time_field_date_time"
        date -> "date_time_field_date"
        else -> "date_time_field_time"
    }
    val text = value?.let { getDateTime(utc = it, parts = DateTimeParts(year = date, month = date, day = date, hour = time, minute = time)) }

    if (!editing) {
        FieldValue(modifier = modifier, label = labelKey, value = text)
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
                text = getTranslation(key = labelKey),
                maxLines = 1,
                style = typography.labelMedium
            )
            Text(
                text = text?.takeIf { it.isNotBlank() } ?: EMPTY_VALUE,
                maxLines = 1,
                style = typography.bodyLarge
            )
        }
        Icon(imageVector = if (!date) Icons.Default.Schedule else Icons.Default.CalendarToday)
    }

    if (pickerOpen) {
        val timeZone = remember { TimeZone.currentSystemDefault() }
        val selected = remember(value) { value?.toInstant()?.toLocalDateTime(timeZone = timeZone) }
        val confirmText = getTranslation(key = confirmLabel)
        val dismissText = getTranslation(key = dismissLabel)
        val onSelected: (LocalDateTime?) -> Unit = { picked ->
            pickerOpen = false
            picked?.let { onValueChange(it.toInstant(timeZone = timeZone).toString()) }
        }

        when {
            date && time -> DateTimePicker(
                selectedDateTime = selected,
                dialog = true,
                confirmText = confirmText,
                dismissText = dismissText,
                onDateTimeSelected = onSelected,
                onDismissRequest = { pickerOpen = false }
            )

            date -> DatePicker(
                selectedDate = selected?.date,
                dialog = true,
                confirmText = confirmText,
                dismissText = dismissText,
                onDateSelected = { picked -> onSelected(picked?.atTime(time = selected?.time ?: LocalTime(hour = 0, minute = 0))) },
                onDismissRequest = { pickerOpen = false }
            )

            else -> TimePicker(
                time = selected?.time ?: LocalTime(hour = 0, minute = 0),
                dialog = true,
                confirmText = confirmText,
                dismissText = dismissText,
                onTimeSelected = { picked -> onSelected((selected?.date ?: now().toLocalDateTime(timeZone = timeZone).date).atTime(time = picked)) },
                onDismissRequest = { pickerOpen = false }
            )
        }
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
        DateTimeField(label = "Date", time = false, value = "2026-11-09T09:00:00Z", editing = true)
        DateTimeField(label = "Hour", date = false, value = "2026-11-09T09:00:00Z", editing = true)
        DateTimeField(label = "Hour", date = false, editing = true)
    }
}
