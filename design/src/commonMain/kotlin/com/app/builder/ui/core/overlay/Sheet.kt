package com.app.builder.ui.core.overlay

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ModalBottomSheetProperties
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.app.builder.ui.Preview
import com.app.builder.ui.core.button.Button
import com.app.builder.ui.core.text.Text
import androidx.compose.material3.ModalBottomSheet as MaterialModalBottomSheet

/**
 * An overlay container.
 *
 * @param modifier The [Modifier] to be applied to the bottom sheet.
 * @param onDismissRequest Callback invoked when the overlay is dismissed by tapping outside or dragging down completely.
 * @param sheetGesturesEnabled Controls whether the overlay sheet allows interaction through mechanical user swipe gestures. Defaults to `true`.
 * @param shouldDismissOnBackPress Determines whether the bottom sheet should close when the user performs a back event. Defaults to `true`.
 * @param shouldDismissOnClickOutside Determines whether the bottom sheet should close when the user clicks outside its bounds. Defaults to `true`.
 * @param sheetMaxWidth The maximum width of the bottom sheet. Defaults to [Dp.Unspecified].
 * @param fullScreen When `true`, the sheet opens straight to occupying the full screen height. Defaults to `false`.
 * @param content A composable slot block executing inside the sheet's sequential [ColumnScope].
 */
@Composable
fun ModalBottomSheet(
    modifier: Modifier = Modifier,
    onDismissRequest: () -> Unit = {},
    sheetGesturesEnabled: Boolean = true,
    shouldDismissOnBackPress: Boolean = true,
    shouldDismissOnClickOutside: Boolean = true,
    sheetMaxWidth: Dp = Dp.Unspecified,
    fullScreen: Boolean = false,
    content: @Composable ColumnScope.() -> Unit = {},
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = fullScreen)

    MaterialModalBottomSheet(
        modifier = modifier,
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        sheetGesturesEnabled = sheetGesturesEnabled,
        properties = ModalBottomSheetProperties(
            shouldDismissOnBackPress = shouldDismissOnBackPress,
            shouldDismissOnClickOutside = shouldDismissOnClickOutside
        ),
        sheetMaxWidth = sheetMaxWidth,
        content = { Column(modifier = Modifier.fillMaxHeight(), content = content) }
    )
}

@Preview
@Composable
private fun ModalBottomSheetPreview() = Preview {
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

@Preview
@Composable
private fun ModalBottomSheetFullScreenPreview() = Preview {
    var opened by remember { mutableStateOf(value = false) }
    Button(text = "Open Full-Screen Modal Sheet", onClick = { opened = true })
    if (opened) {
        ModalBottomSheet(onDismissRequest = { opened = false }, fullScreen = true) {
            Box(modifier = Modifier.fillMaxWidth().padding(all = 24.dp), contentAlignment = Alignment.Center) {
                Text(text = "Full-screen modal bottom sheet content")
            }
        }
    }
}
