package com.app.builder.ui.core.overlay

import kotlin.time.Instant
import kotlinx.coroutines.flow.drop
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.atTime
import kotlinx.datetime.toLocalDateTime
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialogDefaults
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DatePickerState
import androidx.compose.material3.Surface
import androidx.compose.material3.TimeInput
import androidx.compose.material3.TimePickerDialog
import androidx.compose.material3.TimePickerLayoutType
import androidx.compose.material3.TimePickerState
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import com.app.builder.ui.LocalSplitScreen
import com.app.builder.ui.LocalTypography
import com.app.builder.ui.Preview
import com.app.builder.ui.core.button.Button
import com.app.builder.ui.core.text.Text
import androidx.compose.material3.DatePicker as MaterialDatePicker
import androidx.compose.material3.TimePicker as MaterialTimePicker
import androidx.compose.ui.window.Dialog as WindowDialog

/**
 * A calendar grid for selecting a single date, shown inline or inside a modal dialog.
 *
 * @param modifier The [Modifier] applied to the picker when inline, or to the dialog when [dialog] is true.
 * @param selectedDate The pre-selected date. If null, no date is pre-selected.
 * @param dialog Whether the picker is wrapped in a modal dialog with confirm and dismiss buttons.
 * @param confirmText The confirmation text. Used only when [dialog] is true.
 * @param dismissText The cancellation text. Used only when [dialog] is true.
 * @param onDateSelected Callback invoked with the chosen date, or null when none is chosen: on every selection change when inline, or when the user confirms when [dialog] is true.
 * @param onDismissRequest Callback invoked when the user cancels or dismisses the dialog without confirming.
 */
@Composable
fun DatePicker(
    modifier: Modifier = Modifier,
    selectedDate: LocalDate? = null,
    dialog: Boolean = false,
    confirmText: String? = null,
    dismissText: String? = null,
    onDateSelected: (LocalDate?) -> Unit = {},
    onDismissRequest: () -> Unit = {},
) {
    val state = rememberDatePickerState(initialSelectedDateMillis = selectedDate?.toDateMillis())

    if (dialog) {
        DatePickerDialog(
            modifier = modifier,
            onDismissRequest = onDismissRequest,
            confirmButton = {
                confirmText?.let {
                    Button(
                        text = confirmText,
                        onClick = { onDateSelected(state.selectedDateMillis?.toLocalDate()) }
                    )
                }
            },
            dismissButton = {
                dismissText?.let {
                    Button(
                        text = dismissText,
                        onClick = { onDismissRequest() }
                    )
                }
            }
        ) {
            MaterialDatePicker(state = state)
        }
    } else {
        val currentOnDateSelected by rememberUpdatedState(newValue = onDateSelected)
        LaunchedEffect(state) {
            snapshotFlow { state.selectedDateMillis?.toLocalDate() }
                .drop(count = 1)
                .collect { currentOnDateSelected(it) }
        }
        MaterialDatePicker(modifier = modifier, state = state)
    }
}

/**
 * A clock face for selecting an hour and minute, shown inline or inside a modal dialog.
 *
 * @param modifier The [Modifier] applied to the picker when inline, or to the dialog when [dialog] is true.
 * @param title An optional heading rendered at the top of the dialog. Used only when [dialog] is true.
 * @param time The pre-selected time, of which the hour and the minute are used.
 * @param dialog Whether the picker is wrapped in a modal dialog with confirm and dismiss buttons.
 * @param confirmText The confirmation text. Used only when [dialog] is true.
 * @param dismissText The cancellation text. Used only when [dialog] is true.
 * @param onTimeSelected Callback invoked with the chosen hour and minute: on every selection change when inline, or when the user confirms when [dialog] is true.
 * @param onDismissRequest Callback invoked when the user cancels or dismisses the dialog without confirming.
 */
@Composable
fun TimePicker(
    modifier: Modifier = Modifier,
    title: String? = null,
    time: LocalTime = LocalTime(hour = 0, minute = 0),
    dialog: Boolean = false,
    confirmText: String? = null,
    dismissText: String? = null,
    onTimeSelected: (LocalTime) -> Unit = {},
    onDismissRequest: () -> Unit = {},
) {
    val typography = LocalTypography.current
    val state = rememberTimePickerState(initialHour = time.hour, initialMinute = time.minute)

    if (dialog) {
        TimePickerDialog(
            modifier = modifier,
            onDismissRequest = onDismissRequest,
            title = {
                title?.let {
                    Text(
                        text = it,
                        textAlign = TextAlign.Start,
                        style = typography.labelLarge
                    )
                }
            },
            confirmButton = {
                confirmText?.let {
                    Button(
                        text = confirmText,
                        onClick = { onTimeSelected(LocalTime(hour = state.hour, minute = state.minute)) }
                    )
                }
            },
            dismissButton = {
                dismissText?.let {
                    Button(
                        text = dismissText,
                        onClick = { onDismissRequest() }
                    )
                }
            }
        ) {
            MaterialTimePicker(state = state)
        }
    } else {
        val currentOnTimeSelected by rememberUpdatedState(newValue = onTimeSelected)
        LaunchedEffect(state) {
            snapshotFlow { LocalTime(hour = state.hour, minute = state.minute) }
                .drop(count = 1)
                .collect { currentOnTimeSelected(it) }
        }
        MaterialTimePicker(modifier = modifier, state = state)
    }
}

/**
 * A calendar grid and a time selector for selecting a date and a time in one view, shown inline or inside a modal dialog.
 * On a split screen the calendar sits on the left and a clock face on the right; otherwise the calendar sits on top and hour and minute inputs below.
 *
 * @param modifier The [Modifier] applied to the picker when inline, or to the dialog when [dialog] is true.
 * @param selectedDateTime The pre-selected date and time, of which the date, the hour and the minute are used. If null, no date is pre-selected and the time is midnight.
 * @param dialog Whether the picker is wrapped in a modal dialog with confirm and dismiss buttons.
 * @param confirmText The confirmation text. Used only when [dialog] is true.
 * @param dismissText The cancellation text. Used only when [dialog] is true.
 * @param onDateTimeSelected Callback invoked with the chosen date, hour and minute, or null when no date is chosen: on every selection change when inline, or when the user confirms when [dialog] is true.
 * @param onDismissRequest Callback invoked when the user cancels or dismisses the dialog without confirming.
 */
@Composable
fun DateTimePicker(
    modifier: Modifier = Modifier,
    selectedDateTime: LocalDateTime? = null,
    dialog: Boolean = false,
    confirmText: String? = null,
    dismissText: String? = null,
    onDateTimeSelected: (LocalDateTime?) -> Unit = {},
    onDismissRequest: () -> Unit = {},
) {
    val dateState = rememberDatePickerState(initialSelectedDateMillis = selectedDateTime?.date?.toDateMillis())
    val timeState = rememberTimePickerState(initialHour = selectedDateTime?.hour ?: 0, initialMinute = selectedDateTime?.minute ?: 0)
    val picked = { dateState.selectedDateMillis?.toLocalDate()?.atTime(hour = timeState.hour, minute = timeState.minute) }

    if (dialog) {
        WindowDialog(
            onDismissRequest = onDismissRequest,
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Surface(
                modifier = modifier.padding(all = 16.dp),
                shape = AlertDialogDefaults.shape,
                color = AlertDialogDefaults.containerColor,
                tonalElevation = AlertDialogDefaults.TonalElevation
            ) {
                Column(horizontalAlignment = Alignment.End) {
                    DateTimePickerContent(
                        modifier = Modifier
                            .weight(weight = 1f, fill = false)
                            .verticalScroll(state = rememberScrollState()),
                        dateState = dateState,
                        timeState = timeState
                    )
                    Row(
                        modifier = Modifier.padding(start = 8.dp, end = 8.dp, bottom = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(space = 8.dp)
                    ) {
                        dismissText?.let {
                            Button(
                                text = dismissText,
                                onClick = { onDismissRequest() }
                            )
                        }
                        confirmText?.let {
                            Button(
                                text = confirmText,
                                onClick = { onDateTimeSelected(picked()) }
                            )
                        }
                    }
                }
            }
        }
    } else {
        val currentOnDateTimeSelected by rememberUpdatedState(newValue = onDateTimeSelected)
        LaunchedEffect(dateState, timeState) {
            snapshotFlow { picked() }
                .drop(count = 1)
                .collect { currentOnDateTimeSelected(it) }
        }
        DateTimePickerContent(modifier = modifier, dateState = dateState, timeState = timeState)
    }
}

/**
 * The calendar grid and the time selector of a [DateTimePicker], side by side with a clock face on a split screen, or stacked with hour and minute inputs otherwise.
 *
 * @param modifier The [Modifier] applied to the layout.
 * @param dateState The state of the calendar grid.
 * @param timeState The state of the time selector.
 */
@Composable
private fun DateTimePickerContent(
    modifier: Modifier,
    dateState: DatePickerState,
    timeState: TimePickerState,
) {
    if (LocalSplitScreen.current) {
        Row(
            modifier = modifier,
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(space = 24.dp)
        ) {
            MaterialDatePicker(modifier = Modifier.widthIn(max = CALENDAR_MAX_WIDTH), state = dateState)
            MaterialTimePicker(modifier = Modifier.padding(end = 24.dp), state = timeState, layoutType = TimePickerLayoutType.Vertical)
        }
    } else {
        Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
            MaterialDatePicker(modifier = Modifier.widthIn(max = CALENDAR_MAX_WIDTH), state = dateState)
            TimeInput(modifier = Modifier.padding(horizontal = 24.dp), state = timeState)
        }
    }
}

/**
 * Returns this date as the epoch milliseconds of its midnight in UTC.
 *
 * @return The epoch milliseconds of the midnight in UTC of this date.
 */
private fun LocalDate.toDateMillis(): Long = atStartOfDayIn(timeZone = TimeZone.UTC).toEpochMilliseconds()

/**
 * Returns the date of these epoch milliseconds in UTC.
 *
 * @return The date in UTC.
 */
private fun Long.toLocalDate(): LocalDate = Instant.fromEpochMilliseconds(epochMilliseconds = this).toLocalDateTime(timeZone = TimeZone.UTC).date

/** The maximum width of the calendar grid of a [DateTimePicker]. */
private val CALENDAR_MAX_WIDTH = 360.dp

@PreviewLightDark
@Composable
private fun DatePickerDialogPreview() = Preview {
    DatePicker(dialog = true)
}

@PreviewLightDark
@Composable
private fun DatePickerPreview() = Preview {
    DatePicker(dialog = false)
}

@PreviewLightDark
@Composable
private fun TimePickerDialogPreview() = Preview {
    TimePicker(dialog = true)
}

@PreviewLightDark
@Composable
private fun TimePickerPreview() = Preview {
    TimePicker(dialog = false)
}

@PreviewLightDark
@Composable
private fun DateTimePickerDialogPreview() = Preview {
    DateTimePicker(dialog = true)
}

@PreviewLightDark
@Composable
private fun DateTimePickerPreview() = Preview {
    DateTimePicker(dialog = false)
}
