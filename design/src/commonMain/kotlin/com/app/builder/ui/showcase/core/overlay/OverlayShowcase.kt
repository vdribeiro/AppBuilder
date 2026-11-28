package com.app.builder.ui.showcase.core.overlay

import kotlinx.datetime.TimeZone
import kotlinx.datetime.atTime
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.app.builder.core.locale.DateTimeParts
import com.app.builder.core.locale.now
import com.app.builder.ui.core.button.Button
import com.app.builder.ui.core.container.Scaffold
import com.app.builder.ui.core.image.Icon
import com.app.builder.ui.core.overlay.Badge
import com.app.builder.ui.core.overlay.BadgedBox
import com.app.builder.ui.core.overlay.DatePicker
import com.app.builder.ui.core.overlay.DateTimePicker
import com.app.builder.ui.core.overlay.Dialog
import com.app.builder.ui.core.overlay.ModalBottomSheet
import com.app.builder.ui.core.overlay.Position
import com.app.builder.ui.core.overlay.Snackbar
import com.app.builder.ui.core.overlay.TimePicker
import com.app.builder.ui.core.overlay.Tooltip
import com.app.builder.ui.core.text.Text
import com.app.builder.ui.getDateTime
import com.app.builder.ui.showcase.ShowcaseSection

/** Displays badges, date and time pickers, modal bottom sheets, dialogs, snackbars, and tooltips. */
@Composable
fun OverlayShowcase() {
    Column(
        modifier = Modifier.padding(all = 16.dp),
        verticalArrangement = Arrangement.spacedBy(space = 32.dp),
    ) {
        ShowcaseSection(title = "Static badges") { StaticBadgesPreview() }
        ShowcaseSection(title = "Live counter") { LiveCounterBadgePreview() }
        ShowcaseSection(title = "Date picker") { DatePickerPreview() }
        ShowcaseSection(title = "Date picker dialog") { DatePickerDialogPreview() }
        ShowcaseSection(title = "Time picker") { TimePickerPreview() }
        ShowcaseSection(title = "Time picker dialog") { TimePickerDialogPreview() }
        ShowcaseSection(title = "Date time picker") { DateTimePickerPreview() }
        ShowcaseSection(title = "Date time picker dialog") { DateTimePickerDialogPreview() }
        ShowcaseSection(title = "Modal Bottom Sheet") { ModalBottomSheetPreview() }
        ShowcaseSection(title = "Modal Bottom Sheet Full Screen") { ModalBottomSheetFullScreenPreview() }
        ShowcaseSection(title = "Confirmation dialog") { DialogPreview() }
        ShowcaseSection(title = "Snackbar") { SnackbarPreview() }
        ShowcaseSection(title = "Tooltip") { TooltipPreview() }
    }
}

@Composable
private fun StaticBadgesPreview() {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(space = 24.dp)) {
        BadgedBox(badge = { Badge() }) { Icon(imageVector = Icons.Default.Apps) }
        BadgedBox(badge = { Badge(text = "3") }) { Icon(imageVector = Icons.Default.Apps) }
        BadgedBox(badge = { Badge(text = "99+") }) { Icon(imageVector = Icons.Default.Apps) }
        BadgedBox(badge = { Badge(text = "9000") }) { Icon(imageVector = Icons.Default.Apps) }
    }
}

@Composable
private fun LiveCounterBadgePreview() {
    var count by remember { mutableIntStateOf(value = 0) }
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(space = 16.dp)) {
        BadgedBox(badge = { if (count > 0) Badge(text = count.toString()) }) {
            Icon(imageVector = Icons.Default.Apps)
        }
        Button(text = "Add notification", onClick = { count++ })
        Button(text = "Clear", onClick = { count = 0 })
    }
}

@Composable
private fun DatePickerPreview() {
    var selected by remember { mutableStateOf(value = now().toLocalDateTime(timeZone = TimeZone.currentSystemDefault())) }

    Column(verticalArrangement = Arrangement.spacedBy(space = 8.dp)) {
        DatePicker(
            selectedDate = selected.date,
            onDateSelected = { date -> date?.let { selected = it.atTime(time = selected.time) } }
        )
        Text(text = "Selected: ${getDateTime(utc = selected.toInstant(timeZone = TimeZone.currentSystemDefault()).toString(), parts = DateTimeParts(hour = false, minute = false))}")
    }
}

@Composable
private fun DatePickerDialogPreview() {
    var showPicker by remember { mutableStateOf(value = false) }
    var selected by remember { mutableStateOf(value = now().toLocalDateTime(timeZone = TimeZone.currentSystemDefault())) }

    Column(verticalArrangement = Arrangement.spacedBy(space = 8.dp)) {
        Button(text = "Pick a date", onClick = { showPicker = true })
        Text(text = "Selected: ${getDateTime(utc = selected.toInstant(timeZone = TimeZone.currentSystemDefault()).toString(), parts = DateTimeParts(hour = false, minute = false))}")
    }

    if (showPicker) DatePicker(
        selectedDate = selected.date,
        dialog = true,
        confirmText = "Confirm",
        dismissText = "Cancel",
        onDateSelected = { date ->
            date?.let { selected = it.atTime(time = selected.time) }
            showPicker = false
        },
        onDismissRequest = { showPicker = false }
    )
}

@Composable
private fun TimePickerPreview() {
    var selected by remember { mutableStateOf(value = now().toLocalDateTime(timeZone = TimeZone.currentSystemDefault())) }

    Column(verticalArrangement = Arrangement.spacedBy(space = 8.dp)) {
        TimePicker(
            time = selected.time,
            onTimeSelected = { selected = selected.date.atTime(time = it) }
        )
        Text(text = "Selected: ${getDateTime(utc = selected.toInstant(timeZone = TimeZone.currentSystemDefault()).toString(), parts = DateTimeParts(year = false, month = false, day = false))}")
    }
}

@Composable
private fun TimePickerDialogPreview() {
    var showPicker by remember { mutableStateOf(value = false) }
    var selected by remember { mutableStateOf(value = now().toLocalDateTime(timeZone = TimeZone.currentSystemDefault())) }

    Column(verticalArrangement = Arrangement.spacedBy(space = 8.dp)) {
        Button(text = "Pick a time", onClick = { showPicker = true })
        Text(text = "Selected: ${getDateTime(utc = selected.toInstant(timeZone = TimeZone.currentSystemDefault()).toString(), parts = DateTimeParts(year = false, month = false, day = false))}")
    }

    if (showPicker) TimePicker(
        title = "Select time",
        time = selected.time,
        dialog = true,
        confirmText = "Confirm",
        dismissText = "Cancel",
        onTimeSelected = {
            selected = selected.date.atTime(time = it)
            showPicker = false
        },
        onDismissRequest = { showPicker = false }
    )
}

@Composable
private fun DateTimePickerPreview() {
    var selected by remember { mutableStateOf(value = now().toLocalDateTime(timeZone = TimeZone.currentSystemDefault())) }

    Column(verticalArrangement = Arrangement.spacedBy(space = 8.dp)) {
        DateTimePicker(
            selectedDateTime = selected,
            onDateTimeSelected = { dateTime -> dateTime?.let { selected = it } }
        )
        Text(text = "Selected: ${getDateTime(utc = selected.toInstant(timeZone = TimeZone.currentSystemDefault()).toString())}")
    }
}

@Composable
private fun DateTimePickerDialogPreview() {
    var showPicker by remember { mutableStateOf(value = false) }
    var selected by remember { mutableStateOf(value = now().toLocalDateTime(timeZone = TimeZone.currentSystemDefault())) }

    Column(verticalArrangement = Arrangement.spacedBy(space = 8.dp)) {
        Button(text = "Pick a date and time", onClick = { showPicker = true })
        Text(text = "Selected: ${getDateTime(utc = selected.toInstant(timeZone = TimeZone.currentSystemDefault()).toString())}")
    }

    if (showPicker) DateTimePicker(
        selectedDateTime = selected,
        dialog = true,
        confirmText = "Confirm",
        dismissText = "Cancel",
        onDateTimeSelected = { dateTime ->
            dateTime?.let { selected = it }
            showPicker = false
        },
        onDismissRequest = { showPicker = false }
    )
}

@Composable
private fun ModalBottomSheetPreview() {
    var opened by remember { mutableStateOf(value = false) }
    Button(text = "Open Modal Sheet", onClick = { opened = true })
    if (opened) {
        ModalBottomSheet(onDismissRequest = { opened = false }) {
            Box(modifier = Modifier.fillMaxWidth().padding(all = 24.dp), contentAlignment = Alignment.Center) {
                Text(text = "Modal bottom sheet content")
            }
        }
    }
}

@Composable
private fun ModalBottomSheetFullScreenPreview() {
    var opened by remember { mutableStateOf(value = false) }
    Button(text = "Open Full-Screen Modal Sheet", onClick = { opened = true })
    if (opened) {
        ModalBottomSheet(onDismissRequest = { opened = false }, fullScreen = true) {
            Box(modifier = Modifier.fillMaxWidth().padding(all = 24.dp), contentAlignment = Alignment.Center) {
                Text(text = "Full screen modal bottom sheet content")
            }
        }
    }
}

@Composable
private fun DialogPreview() {
    var showDialog by remember { mutableStateOf(value = false) }
    var result by remember { mutableStateOf<String?>(value = null) }

    Column(verticalArrangement = Arrangement.spacedBy(space = 8.dp)) {
        Button(text = "Open dialog", onClick = { showDialog = true })
        result?.let { Text(text = "Result: $it") }
    }

    if (showDialog) {
        Dialog(
            title = "Delete item?",
            text = "This action cannot be undone.",
            confirmText = "Delete",
            dismissText = "Cancel",
            onConfirm = {
                result = "Confirmed"
                showDialog = false
            },
            onDismiss = {
                result = "Dismissed"
                showDialog = false
            },
        )
    }
}

@Composable
private fun SnackbarPreview() {
    var messageId by remember { mutableIntStateOf(value = 0) }

    Box(modifier = Modifier.height(height = 150.dp)) {
        Scaffold(
            content = { innerPadding ->
                Button(
                    modifier = Modifier.padding(paddingValues = innerPadding),
                    text = "Show snackbar",
                    onClick = { messageId++ }
                )
            },
            snackbarHost = { if (messageId > 0) Snackbar(message = "Action #$messageId completed", buttonText = "Undo") }
        )
    }
}

@Composable
private fun TooltipPreview() {
    Column(verticalArrangement = Arrangement.spacedBy(space = 8.dp)) {
        Text(text = "Hover or long-press the icon")
        Position.entries.forEach {
            Tooltip(text = it.name, position = it) {
                Button(content = { Icon(imageVector = Icons.Default.Apps) })
            }
        }
    }
}

