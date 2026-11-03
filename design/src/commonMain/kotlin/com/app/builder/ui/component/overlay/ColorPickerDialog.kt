package com.app.builder.ui.component.overlay

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.app.builder.ui.LocalColorScheme
import com.app.builder.ui.LocalShapes
import com.app.builder.ui.Preview
import com.app.builder.ui.core.button.Button
import com.app.builder.ui.core.button.ButtonStyle
import com.app.builder.ui.core.overlay.Dialog
import com.app.builder.ui.core.text.Input
import com.app.builder.ui.toColorOrNull
import com.app.builder.ui.toHex

/** The lightness of each shade in a palette row, from lightest to darkest. */
private val paletteLightness = listOf(0.92f, 0.84f, 0.74f, 0.64f, 0.54f, 0.44f, 0.36f, 0.28f, 0.2f, 0.12f)

/** The selectable colors, one row of shades per hue every 20 degrees, followed by a row of greys. */
private val palette: List<List<Color>> = buildList {
    (0 until 360 step 20).forEach { hue ->
        add(element = paletteLightness.map { Color.hsl(hue = hue.toFloat(), saturation = 0.7f, lightness = it) })
    }
    add(element = paletteLightness.map { Color.hsl(hue = 0f, saturation = 0f, lightness = it) })
}

/**
 * A dialog for choosing a color, either from a palette or by typing its hexadecimal value.
 *
 * @param modifier The [Modifier] to be applied to the dialog.
 * @param title The heading rendered at the top of the dialog.
 * @param color The color currently in use, preselected when the dialog opens.
 * @param onConfirm Invoked with the chosen color when the user applies it.
 * @param onReset Invoked when the user restores the default color.
 * @param onDismiss Invoked when the user closes the dialog without changing the color.
 */
@Composable
fun ColorPickerDialog(
    modifier: Modifier = Modifier,
    title: String,
    color: Color,
    onConfirm: (Color) -> Unit = {},
    onReset: () -> Unit = {},
    onDismiss: () -> Unit = {},
) {
    val colorScheme = LocalColorScheme.current
    val shapes = LocalShapes.current
    var hex by remember { mutableStateOf(value = color.toHex()) }
    val selected = hex.toColorOrNull()

    Dialog(
        modifier = modifier.testTag(tag = "color_picker_dialog"),
        title = title,
        confirmEnabled = selected != null,
        confirmText = "apply",
        dismissText = "cancel",
        onConfirm = { selected?.let(onConfirm) },
        onDismiss = onDismiss
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(space = 12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth().height(height = 40.dp),
                horizontalArrangement = Arrangement.spacedBy(space = 8.dp)
            ) {
                listOf(color, selected ?: color).forEach { swatch ->
                    Box(
                        modifier = Modifier
                            .weight(weight = 1f)
                            .height(height = 40.dp)
                            .background(color = swatch, shape = shapes.small)
                            .border(width = 1.dp, color = colorScheme.outline, shape = shapes.small)
                    )
                }
            }
            Input(
                modifier = Modifier.fillMaxWidth().testTag(tag = "color_picker_hex"),
                value = hex,
                onValueChange = { hex = it },
                maxLines = 1,
                isError = selected == null
            )
            Button(style = ButtonStyle.TEXT, text = "reset", onClick = { onReset() })
            Column(
                modifier = Modifier.heightIn(max = 240.dp).verticalScroll(state = rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(space = 4.dp)
            ) {
                palette.forEach { shades ->
                    Row(horizontalArrangement = Arrangement.spacedBy(space = 4.dp)) {
                        shades.forEach { shade ->
                            val shadeHex = shade.toHex()
                            Box(
                                modifier = Modifier
                                    .weight(weight = 1f)
                                    .aspectRatio(ratio = 1f)
                                    .background(color = shade, shape = shapes.extraSmall)
                                    .then(
                                        other = if (shadeHex == selected?.toHex()) {
                                            Modifier.border(width = 2.dp, color = colorScheme.onSurface, shape = shapes.extraSmall)
                                        } else Modifier
                                    )
                                    .clickable { hex = shadeHex }
                                    .testTag(tag = "color_picker_$shadeHex")
                            )
                        }
                    }
                }
            }
        }
    }
}

@PreviewLightDark
@Composable
private fun ColorPickerDialogPreview() = Preview {
    ColorPickerDialog(title = "Primary", color = LocalColorScheme.current.primary)
}
