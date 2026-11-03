package com.app.builder.ui.component.overlay

import kotlin.math.roundToInt
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.app.builder.ui.LocalColorScheme
import com.app.builder.ui.Preview
import com.app.builder.ui.core.button.Button
import com.app.builder.ui.core.button.ButtonStyle
import com.app.builder.ui.core.overlay.Dialog
import com.app.builder.ui.core.progress.Slider
import com.app.builder.ui.core.text.Text

/** The largest corner radius selectable, in dp. */
private const val MAXIMUM_RADIUS = 50

/**
 * A dialog for choosing a shape's corner radius, previewing the shape as it changes.
 *
 * @param modifier The [Modifier] to be applied to the dialog.
 * @param title The heading rendered at the top of the dialog.
 * @param radius The corner radius currently in use, preselected when the dialog opens.
 * @param onConfirm Invoked with the chosen corner radius when the user applies it.
 * @param onReset Invoked when the user restores the default corner radius.
 * @param onDismiss Invoked when the user closes the dialog without changing the corner radius.
 */
@Composable
fun ShapePickerDialog(
    modifier: Modifier = Modifier,
    title: String,
    radius: Dp,
    onConfirm: (Dp) -> Unit = {},
    onReset: () -> Unit = {},
    onDismiss: () -> Unit = {},
) {
    val colorScheme = LocalColorScheme.current
    var selected by remember { mutableFloatStateOf(value = radius.value.roundToInt().toFloat()) }

    Dialog(
        modifier = modifier.testTag(tag = "shape_picker_dialog"),
        title = title,
        confirmText = "apply",
        dismissText = "cancel",
        onConfirm = { onConfirm(selected.dp) },
        onDismiss = onDismiss
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(space = 12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(size = 96.dp)
                    .background(color = colorScheme.primaryContainer, shape = RoundedCornerShape(size = selected.dp))
            )
            Text(text = "${selected.roundToInt()}dp", translate = false)
            Slider(
                modifier = Modifier.fillMaxWidth().testTag(tag = "shape_picker_radius"),
                value = selected,
                onValueChange = { selected = it.roundToInt().toFloat() },
                valueRange = 0f..MAXIMUM_RADIUS.toFloat(),
                steps = MAXIMUM_RADIUS - 1
            )
            Button(style = ButtonStyle.TEXT, text = "reset", onClick = { onReset() })
        }
    }
}

@PreviewLightDark
@Composable
private fun ShapePickerDialogPreview() = Preview {
    ShapePickerDialog(title = "Medium", radius = 12.dp)
}
