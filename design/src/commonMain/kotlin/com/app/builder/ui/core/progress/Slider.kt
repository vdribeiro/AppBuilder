package com.app.builder.ui.core.progress

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.app.builder.ui.Preview
import com.app.builder.ui.core.text.Text
import androidx.compose.material3.Slider as MaterialSlider

/**
 * Slider component that lets users make a selection from a continuous or stepped range of values.
 *
 * @param modifier The [Modifier] to be applied to the layout of the slider.
 * @param enabled Controls the enabled state of the slider. When `false`, user interactions are disabled.
 * @param value The current value of the slider. Coerced to fit within [valueRange].
 * @param onValueChange Callback invoked continuously while the user drags the value.
 * @param valueRange The inclusive range of values that this slider can take.
 * @param steps If positive, the amount of discrete allowable values between the endpoints of [valueRange], evenly distributed. If `0`, the slider allows any value from the range continuously.
 * @param onValueChangeFinished Callback invoked once the user has finished selecting a new value, after a drag or a click.
 */
@Composable
fun Slider(
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    value: Float = 0f,
    onValueChange: (Float) -> Unit = {},
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
    steps: Int = 0,
    onValueChangeFinished: (() -> Unit)? = null,
) {
    MaterialSlider(
        modifier = modifier,
        enabled = enabled,
        value = value.coerceIn(range = valueRange),
        onValueChange = onValueChange,
        valueRange = valueRange,
        steps = steps.coerceAtLeast(minimumValue = 0),
        onValueChangeFinished = onValueChangeFinished
    )
}

@Preview
@Composable
private fun SliderPreview() = Preview {
    var value by remember { mutableFloatStateOf(value = 0.5f) }
    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(space = 8.dp)) {
        Text(text = "Value: ${(value * 100).toInt()}%")
        Slider(
            modifier = Modifier.fillMaxWidth(),
            value = value,
            onValueChange = { value = it }
        )
    }
}

@Preview
@Composable
private fun SteppedSliderPreview() = Preview {
    var value by remember { mutableFloatStateOf(value = 2f) }
    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(space = 8.dp)) {
        Text(text = "Value: ${value.toInt()}")
        Slider(
            modifier = Modifier.fillMaxWidth(),
            value = value,
            onValueChange = { value = it },
            valueRange = 0f..4f,
            steps = 3
        )
    }
}
