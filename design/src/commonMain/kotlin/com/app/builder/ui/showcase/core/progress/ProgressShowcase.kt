package com.app.builder.ui.showcase.core.progress

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.app.builder.ui.core.progress.ProgressIndicator
import com.app.builder.ui.core.progress.Slider
import com.app.builder.ui.core.text.Text
import com.app.builder.ui.showcase.ShowcaseSection

/** Displays circular and linear progress indicators alongside continuous and stepped sliders. */
@Composable
fun ProgressShowcase() {
    Column(
        modifier = Modifier.padding(all = 16.dp),
        verticalArrangement = Arrangement.spacedBy(space = 32.dp),
    ) {
        ShowcaseSection(title = "Circular") { ProgressIndicatorCircularPreview() }
        ShowcaseSection(title = "Linear") { ProgressIndicatorLinearPreview() }
        ShowcaseSection(title = "Continuous Slider") { ContinuousSliderPreview() }
        ShowcaseSection(title = "Stepped Slider") { SteppedSliderPreview() }
    }
}

@Composable
private fun ProgressIndicatorCircularPreview() {
    Column(verticalArrangement = Arrangement.spacedBy(space = 8.dp)) {
        ProgressIndicator(circular = true)
        ProgressIndicator(progress = 0.0f, circular = true)
        ProgressIndicator(progress = 0.5f, circular = true)
        ProgressIndicator(progress = 1.0f, circular = true)
    }
}

@Composable
private fun ProgressIndicatorLinearPreview() {
    Column(verticalArrangement = Arrangement.spacedBy(space = 8.dp)) {
        ProgressIndicator(circular = false)
        ProgressIndicator(progress = 0.0f, circular = false)
        ProgressIndicator(progress = 0.5f, circular = false)
        ProgressIndicator(progress = 1.0f, circular = false)
    }
}

@Composable
private fun ContinuousSliderPreview() {
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

@Composable
private fun SteppedSliderPreview() {
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
