package com.app.builder.ui.core.overlay

import kotlin.time.Instant
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.TimePickerDialog
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.PreviewLightDark
import com.app.builder.core.locale.toInstant
import com.app.builder.ui.LocalTypography
import com.app.builder.ui.Preview
import com.app.builder.ui.core.button.Button
import com.app.builder.ui.core.text.Text
import kotlinx.coroutines.flow.drop
import androidx.compose.material3.DatePicker as MaterialDatePicker
import androidx.compose.material3.TimePicker as MaterialTimePicker

/**
 * A calendar grid for selecting a single date, shown inline or inside a modal dialog.
 *
 * @param modifier The [Modifier] applied to the picker when inline, or to the dialog when [dialog] is true.
 * @param selectedDate The currently selected date. If null, no date is pre-selected.
 * @param dialog Whether the picker is wrapped in a modal dialog with confirm and dismiss buttons.
 * @param confirmText The confirmation text. Used only when [dialog] is true.
 * @param dismissText The cancellation text. Used only when [dialog] is true.
 * @param onDateSelected Callback invoked with the chosen date: on every selection change when inline, or when the
 * user confirms when [dialog] is true.
 * @param onDismissRequest Callback invoked when the user cancels or dismisses the dialog without confirming.
 */
@Composable
fun DatePicker(
    modifier: Modifier = Modifier,
    selectedDate: Instant? = null,
    dialog: Boolean = false,
    confirmText: String? = null,
    dismissText: String? = null,
    onDateSelected: (Instant?) -> Unit = {},
    onDismissRequest: () -> Unit = {},
) {
    val state = rememberDatePickerState(initialSelectedDateMillis = selectedDate?.toEpochMilliseconds())

    if (dialog) {
        DatePickerDialog(
            modifier = modifier,
            onDismissRequest = onDismissRequest,
            confirmButton = {
                confirmText?.let {
                    Button(
                        text = confirmText,
                        onClick = { onDateSelected(state.selectedDateMillis?.toInstant()) }
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
            snapshotFlow { state.selectedDateMillis }
                .drop(count = 1)
                .collect { currentOnDateSelected(it?.toInstant()) }
        }
        MaterialDatePicker(modifier = modifier, state = state)
    }
}

/**
 * A clock face for selecting an hour and minute, shown inline or inside a modal dialog.
 *
 * @param modifier The [Modifier] applied to the picker when inline, or to the dialog when [dialog] is true.
 * @param title An optional heading rendered at the top of the dialog. Used only when [dialog] is true.
 * @param initialHour The pre-selected hour, in 24-hour format (0-23).
 * @param initialMinute The pre-selected minute (0-59).
 * @param dialog Whether the picker is wrapped in a modal dialog with confirm and dismiss buttons.
 * @param confirmText The confirmation text. Used only when [dialog] is true.
 * @param dismissText The cancellation text. Used only when [dialog] is true.
 * @param onTimeSelected Callback invoked with the chosen hour and minute: on every selection change when inline, or
 * when the user confirms when [dialog] is true.
 * @param onDismissRequest Callback invoked when the user cancels or dismisses the dialog without confirming.
 */
@Composable
fun TimePicker(
    modifier: Modifier = Modifier,
    title: String? = null,
    initialHour: Int = 0,
    initialMinute: Int = 0,
    dialog: Boolean = false,
    confirmText: String? = null,
    dismissText: String? = null,
    onTimeSelected: (hour: Int, minute: Int) -> Unit = { _, _ -> },
    onDismissRequest: () -> Unit = {},
) {
    val typography = LocalTypography.current
    val state = rememberTimePickerState(
        initialHour = initialHour.coerceIn(minimumValue = 0, maximumValue = 23),
        initialMinute = initialMinute.coerceIn(minimumValue = 0, maximumValue = 59)
    )

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
                        onClick = { onTimeSelected(state.hour, state.minute) }
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
            snapshotFlow { state.hour to state.minute }
                .drop(count = 1)
                .collect { (hour, minute) -> currentOnTimeSelected(hour, minute) }
        }
        MaterialTimePicker(modifier = modifier, state = state)
    }
}

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
