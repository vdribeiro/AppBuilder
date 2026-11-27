package com.app.builder.ui.core.overlay

import kotlin.time.Instant
import kotlinx.coroutines.flow.drop
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.DatePickerState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.TimeInput
import androidx.compose.material3.TimePickerLayoutType
import androidx.compose.material3.TimePickerState
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import com.app.builder.core.locale.now
import com.app.builder.core.locale.toInstant
import com.app.builder.ui.DateTimeConverter
import com.app.builder.ui.LocalDateTimeConverter
import com.app.builder.ui.LocalTypography
import com.app.builder.ui.Preview
import com.app.builder.ui.core.button.Button
import com.app.builder.ui.core.text.Text
import androidx.compose.material3.DatePicker as MaterialDatePicker
import androidx.compose.material3.TimePicker as MaterialTimePicker
import androidx.compose.ui.window.Dialog as WindowDialog

/**
 * A calendar grid for selecting the date of a moment, keeping its hour and minute.
 * The date shows in the local time zone of the [LocalDateTimeConverter].
 * As a [dialog], the chosen moment is reported when the user confirms. Otherwise the calendar shows in place and every selection is reported.
 *
 * @param modifier The [Modifier] to be applied to the picker's structural container layout.
 * @param selected The currently selected moment. If null, no date is pre-selected and the time of day is midnight.
 * @param dialog Whether the picker shows as a modal dialog with its confirm and dismiss buttons.
 * @param confirmText The confirmation text.
 * @param dismissText The cancellation text.
 * @param onSelected Callback invoked with the chosen date at the hour and minute of [selected].
 * @param onDismissRequest Callback invoked when the user cancels or dismisses the dialog without confirming.
 */
@Composable
fun DatePicker(
    modifier: Modifier = Modifier,
    selected: Instant? = null,
    dialog: Boolean = true,
    confirmText: String? = null,
    dismissText: String? = null,
    onSelected: (Instant) -> Unit = {},
    onDismissRequest: () -> Unit = {},
) {
    val converter = LocalDateTimeConverter.current
    val local = rememberLocal(selected = selected, converter = converter)
    val state = rememberDatePickerState(initialSelectedDateMillis = local?.date()?.toPickerMillis())
    val picked = { state.date()?.let { toInstant(date = it, time = local?.time() ?: MIDNIGHT, converter = converter) } }

    if (!dialog) {
        val onSelectedState by rememberUpdatedState(newValue = onSelected)
        DateChanges(state = state, onChange = { picked()?.let { onSelectedState(it) } })
        MaterialDatePicker(modifier = modifier, state = state)
        return
    }
    PickerDialog(
        modifier = modifier,
        confirmEnabled = state.selectedDateMillis != null,
        confirmText = confirmText,
        dismissText = dismissText,
        onConfirm = { picked()?.let { onSelected(it) } },
        onDismissRequest = onDismissRequest
    ) {
        MaterialDatePicker(state = state)
    }
}

/**
 * A clock face, or a pair of number inputs, for selecting the time of day of a moment, keeping its date.
 * The time of day shows in the local time zone of the [LocalDateTimeConverter].
 * As a [dialog], the chosen moment is reported when the user confirms. Otherwise the picker shows in place and every change is reported.
 *
 * @param modifier The [Modifier] to be applied to the picker's structural container layout.
 * @param title An optional heading title text string rendered at the top of the dialog.
 * @param selected The currently selected moment. If null, the time of day starts at midnight and the date is today.
 * @param dialog Whether the picker shows as a modal dialog with its title and its confirm and dismiss buttons.
 * @param input Whether the hour and minute are typed into number inputs, in place of the clock face.
 * @param confirmText The confirmation text.
 * @param dismissText The cancellation text.
 * @param onSelected Callback invoked with the chosen time of day on the date of [selected], at the start of the chosen minute.
 * @param onDismissRequest Callback invoked when the user cancels or dismisses the dialog without confirming.
 */
@Composable
fun TimePicker(
    modifier: Modifier = Modifier,
    title: String? = null,
    selected: Instant? = null,
    dialog: Boolean = true,
    input: Boolean = false,
    confirmText: String? = null,
    dismissText: String? = null,
    onSelected: (Instant) -> Unit = {},
    onDismissRequest: () -> Unit = {},
) {
    val converter = LocalDateTimeConverter.current
    val local = rememberLocal(selected = selected, converter = converter)
    val state = rememberTimePickerState(initialHour = local?.hour() ?: 0, initialMinute = local?.minute() ?: 0)
    val picked = { (local ?: converter.toLocal(now().toString()).takeIf { it.length >= LOCAL_LENGTH })?.let { toInstant(date = it.date(), time = state.time(), converter = converter) } }

    if (!dialog) {
        val onSelectedState by rememberUpdatedState(newValue = onSelected)
        TimeChanges(state = state, onChange = { picked()?.let { onSelectedState(it) } })
        TimeContent(modifier = modifier, state = state, input = input)
        return
    }
    PickerDialog(
        modifier = modifier,
        title = title,
        confirmText = confirmText,
        dismissText = dismissText,
        onConfirm = { picked()?.let { onSelected(it) } },
        onDismissRequest = onDismissRequest
    ) {
        TimeContent(modifier = Modifier.padding(horizontal = 24.dp), state = state, input = input)
    }
}

/**
 * A calendar grid above a pair of number inputs, for selecting the date and the time of day of a moment together.
 * The date and time show in the local time zone of the [LocalDateTimeConverter].
 * As a [dialog], the chosen moment is reported when the user confirms. Otherwise the picker shows in place and every change is reported.
 *
 * @param modifier The [Modifier] to be applied to the picker's structural container layout.
 * @param selected The currently selected moment. If null, no date is pre-selected and the time of day starts at midnight.
 * @param dialog Whether the picker shows as a modal dialog with its confirm and dismiss buttons.
 * @param confirmText The confirmation text.
 * @param dismissText The cancellation text.
 * @param onSelected Callback invoked with the chosen date and time, at the start of the chosen minute.
 * @param onDismissRequest Callback invoked when the user cancels or dismisses the dialog without confirming.
 */
@Composable
fun DateTimePicker(
    modifier: Modifier = Modifier,
    selected: Instant? = null,
    dialog: Boolean = true,
    confirmText: String? = null,
    dismissText: String? = null,
    onSelected: (Instant) -> Unit = {},
    onDismissRequest: () -> Unit = {},
) {
    val converter = LocalDateTimeConverter.current
    val local = rememberLocal(selected = selected, converter = converter)
    val dateState = rememberDatePickerState(initialSelectedDateMillis = local?.date()?.toPickerMillis())
    val timeState = rememberTimePickerState(initialHour = local?.hour() ?: 0, initialMinute = local?.minute() ?: 0)
    val picked = { dateState.date()?.let { toInstant(date = it, time = timeState.time(), converter = converter) } }
    val content: @Composable () -> Unit = {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            MaterialDatePicker(state = dateState)
            TimeInput(state = timeState)
        }
    }

    if (!dialog) {
        val onSelectedState by rememberUpdatedState(newValue = onSelected)
        DateChanges(state = dateState, onChange = { picked()?.let { onSelectedState(it) } })
        TimeChanges(state = timeState, onChange = { picked()?.let { onSelectedState(it) } })
        Column(modifier = modifier) { content() }
        return
    }
    PickerDialog(
        modifier = modifier,
        confirmEnabled = dateState.selectedDateMillis != null,
        confirmText = confirmText,
        dismissText = dismissText,
        onConfirm = { picked()?.let { onSelected(it) } },
        onDismissRequest = onDismissRequest,
        content = content
    )
}

/**
 * The modal dialog shared by the pickers: an optional title, the picker, and its confirm and dismiss buttons.
 * It is as tall as its content and scrolls when the screen is shorter.
 *
 * @param modifier The [Modifier] to be applied to the dialog's container.
 * @param title An optional heading title text string rendered at the top of the dialog.
 * @param confirmEnabled Controls whether the confirmation button is clickable.
 * @param confirmText The confirmation text, or `null` to hide the button.
 * @param dismissText The cancellation text, or `null` to hide the button.
 * @param onConfirm Callback invoked when the user confirms.
 * @param onDismissRequest Callback invoked when the user cancels or dismisses the dialog without confirming.
 * @param content The picker.
 */
@Composable
private fun PickerDialog(
    modifier: Modifier = Modifier,
    title: String? = null,
    confirmEnabled: Boolean = true,
    confirmText: String? = null,
    dismissText: String? = null,
    onConfirm: () -> Unit = {},
    onDismissRequest: () -> Unit = {},
    content: @Composable () -> Unit,
) {
    val typography = LocalTypography.current

    WindowDialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(
            modifier = modifier.padding(all = 16.dp),
            shape = MaterialTheme.shapes.extraLarge,
            color = DatePickerDefaults.colors().containerColor,
        ) {
            Column(
                modifier = Modifier.verticalScroll(state = rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                title?.let {
                    Text(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 24.dp, end = 24.dp, top = 24.dp, bottom = 16.dp),
                        text = it,
                        textAlign = TextAlign.Start,
                        style = typography.labelLarge
                    )
                }
                content()
                Row(
                    modifier = Modifier
                        .align(alignment = Alignment.End)
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(space = 8.dp),
                ) {
                    dismissText?.let { Button(text = it, onClick = { onDismissRequest() }) }
                    confirmText?.let { Button(enabled = confirmEnabled, text = it, onClick = { onConfirm() }) }
                }
            }
        }
    }
}

/**
 * The clock face, laid out vertically, or the pair of number inputs of a time picker.
 *
 * @param modifier The [Modifier] to be applied to the picker.
 * @param state The hour and minute being picked.
 * @param input Whether the hour and minute are typed into number inputs, in place of the clock face.
 */
@Composable
private fun TimeContent(modifier: Modifier = Modifier, state: TimePickerState, input: Boolean) {
    if (input) TimeInput(modifier = modifier, state = state) else MaterialTimePicker(modifier = modifier, state = state, layoutType = TimePickerLayoutType.Vertical)
}

/**
 * Calls [onChange] for every date selected in [state] after the first one it holds.
 *
 * @param state The date being picked.
 * @param onChange Callback invoked when the selected date changes.
 */
@Composable
private fun DateChanges(state: DatePickerState, onChange: () -> Unit) {
    val onChangeState by rememberUpdatedState(newValue = onChange)
    LaunchedEffect(key1 = state) {
        snapshotFlow { state.selectedDateMillis }.drop(count = 1).collect { onChangeState() }
    }
}

/**
 * Calls [onChange] for every hour and minute set in [state] after the first ones it holds.
 *
 * @param state The hour and minute being picked.
 * @param onChange Callback invoked when the hour or the minute changes.
 */
@Composable
private fun TimeChanges(state: TimePickerState, onChange: () -> Unit) {
    val onChangeState by rememberUpdatedState(newValue = onChange)
    LaunchedEffect(key1 = state) {
        snapshotFlow { state.hour to state.minute }.drop(count = 1).collect { onChangeState() }
    }
}

/**
 * Remembers [selected] as a date and time of day in the local time zone of [converter].
 *
 * @param selected The moment to convert.
 * @param converter Converts the moment into the local date and time.
 * @return The local date-time as an ISO-8601 string without seconds or offset, or `null` when there is no moment or it cannot be converted.
 */
@Composable
private fun rememberLocal(selected: Instant?, converter: DateTimeConverter): String? =
    remember(key1 = selected, key2 = converter) { selected?.let { converter.toLocal(it.toString()) }?.takeIf { it.length >= LOCAL_LENGTH } }

/**
 * Joins a local date and time of day into a moment.
 *
 * @param date The local date as an ISO-8601 string.
 * @param time The local time of day as an ISO-8601 string without seconds.
 * @param converter Converts the local date and time into UTC.
 * @return The moment, or `null` when the date and time are invalid.
 */
private fun toInstant(date: String, time: String, converter: DateTimeConverter): Instant? = converter.toUtc("${date}T$time").toInstant()

/** The date of this local ISO-8601 date-time, such as `2026-11-27`. */
private fun String.date(): String = substring(startIndex = 0, endIndex = DATE_LENGTH)

/** The time of day of this local ISO-8601 date-time, such as `01:15`. */
private fun String.time(): String = substring(startIndex = DATE_LENGTH + 1, endIndex = LOCAL_LENGTH)

/** The hour of this local ISO-8601 date-time. */
private fun String.hour(): Int? = substring(startIndex = DATE_LENGTH + 1, endIndex = DATE_LENGTH + 3).toIntOrNull()

/** The minute of this local ISO-8601 date-time. */
private fun String.minute(): Int? = substring(startIndex = DATE_LENGTH + 4, endIndex = LOCAL_LENGTH).toIntOrNull()

/** The start of the day of this ISO-8601 date in UTC, in milliseconds from the Unix epoch, as a [DatePickerState] holds a date. */
private fun String.toPickerMillis(): Long? = "${this}T00:00:00Z".toInstant()?.toEpochMilliseconds()

/** The date selected in this state as an ISO-8601 string, or `null` when none is. */
private fun DatePickerState.date(): String? = selectedDateMillis?.toInstant()?.toString()?.take(n = DATE_LENGTH)

/** The hour and minute of this state as an ISO-8601 time of day without seconds. */
private fun TimePickerState.time(): String = "${hour.toString().padStart(length = 2, padChar = '0')}:${minute.toString().padStart(length = 2, padChar = '0')}"

/** The time of day at the start of a day. */
private const val MIDNIGHT = "00:00"

/** The length of an ISO-8601 date, such as `2026-11-27`. */
private const val DATE_LENGTH = 10

/** The length of a local ISO-8601 date-time without seconds, such as `2026-11-27T01:15`. */
private const val LOCAL_LENGTH = 16

@PreviewLightDark
@Composable
private fun DatePickerPreview() = Preview {
    DatePicker(
        confirmText = "Confirm",
        dismissText = "Cancel",
        dialog = false
    )
}

@PreviewLightDark
@Composable
private fun DatePickerDialogPreview() = Preview {
    DatePicker(
        confirmText = "Confirm",
        dismissText = "Cancel",
        dialog = true
    )
}

@PreviewLightDark
@Composable
private fun TimePickerPreview() = Preview {
    TimePicker(
        title = "Select time",
        confirmText = "Confirm",
        dismissText = "Cancel",
        dialog = false
    )
}

@PreviewLightDark
@Composable
private fun TimePickerDialogPreview() = Preview {
    TimePicker(
        title = "Select time",
        confirmText = "Confirm",
        dismissText = "Cancel",
        dialog = true
    )
}

@PreviewLightDark
@Composable
private fun DateTimePickerPreview() = Preview {
    DateTimePicker(
        dialog = false,
    )
}

@PreviewLightDark
@Composable
private fun DateTimePickerDialogPreview() = Preview {
    DateTimePicker(
        dialog = true,
    )
}
