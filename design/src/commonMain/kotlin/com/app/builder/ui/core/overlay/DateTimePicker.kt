package com.app.builder.ui.core.overlay

import kotlin.time.Instant
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.TimePickerDialog
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import com.app.builder.core.locale.toInstant
import com.app.builder.ui.LocalTypography
import com.app.builder.ui.Preview
import com.app.builder.ui.core.button.Button
import com.app.builder.ui.core.text.Text
import androidx.compose.material3.DatePicker as MaterialDatePicker
import androidx.compose.material3.TimePicker as MaterialTimePicker

/**
 * A modal dialog presenting a calendar grid for selecting a single date.
 *
 * @param modifier The [Modifier] to be applied to the picker's structural container layout.
 * @param selectedDate The currently selected date. If null, no date is pre-selected.
 * @param confirmText The confirmation text.
 * @param dismissText The cancellation text.
 * @param onDateSelected Callback invoked with the chosen date when the user confirms.
 * @param onDismissRequest Callback invoked when the user cancels or dismisses the picker without confirming.
 */
@Composable
fun DatePicker(
    modifier: Modifier = Modifier,
    selectedDate: Instant? = null,
    confirmText: String? = null,
    dismissText: String? = null,
    onDateSelected: (Instant?) -> Unit = {},
    onDismissRequest: () -> Unit = {},
) {
    val state = rememberDatePickerState(initialSelectedDateMillis = selectedDate?.toEpochMilliseconds())
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
}

/**
 * A modal dialog presenting a clock face for selecting an hour and minute.
 *
 * @param modifier The [Modifier] to be applied to the picker's structural container layout.
 * @param title An optional heading title text string rendered at the top of the dialog.
 * @param initialHour The pre-selected hour, in 24-hour format (0-23).
 * @param initialMinute The pre-selected minute (0-59).
 * @param confirmText The confirmation text.
 * @param dismissText The cancellation text.
 * @param onTimeSelected Callback invoked with the chosen hour and minute when the user confirms.
 * @param onDismissRequest Callback invoked when the user cancels or dismisses the picker without confirming.
 */
@Composable
fun TimePicker(
    modifier: Modifier = Modifier,
    title: String? = null,
    initialHour: Int = 0,
    initialMinute: Int = 0,
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
}

@Preview
@Composable
private fun DatePickerPreview() = Preview {
    DatePicker(
        confirmText = "Confirm",
        dismissText = "Cancel",
    )
}

@Preview
@Composable
private fun TimePickerPreview() = Preview {
    TimePicker(
        title = "Select time",
        confirmText = "Confirm",
        dismissText = "Cancel",
    )
}

private const val TAG = "DateTimePicker"