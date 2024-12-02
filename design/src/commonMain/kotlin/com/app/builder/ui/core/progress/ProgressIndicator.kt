package com.app.builder.ui.core.progress

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ProgressIndicatorDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.app.builder.ui.Preview

/**
 * A component that supports both explicit progress states and infinite indeterminate tracking loops.
 * When a non-null [progress] value is passed, the component tracks value changes smoothly by intercepting metrics using an asynchronous, state-driven [animateFloatAsState] transition curve.
 *
 * @param modifier The [Modifier] to apply to the resolved progress component's layout surface bounds.
 * @param progress The explicit completeness fraction ranging from 0.0 to 1.0. If null, the indicator runs indefinitely as an indeterminate loading animation sequence. Defaults to null.
 * @param circular Dictates the shape format of the drawing asset tracking completion. If `true`, outputs a [CircularProgressIndicator]; if `false`, outputs a [LinearProgressIndicator]. Defaults to `true`.
 */
@Composable
fun ProgressIndicator(
    modifier: Modifier = Modifier,
    progress: Float? = null,
    circular: Boolean = true,
) {
    when {
        progress != null -> {
            val animatedProgress by animateFloatAsState(
                targetValue = progress.coerceIn(0.0f, 1.0f),
                animationSpec = ProgressIndicatorDefaults.ProgressAnimationSpec,
            )
            when {
                circular -> CircularProgressIndicator(
                    modifier = modifier,
                    progress = { animatedProgress },
                )

                else -> LinearProgressIndicator(
                    modifier = modifier,
                    progress = { animatedProgress },
                )
            }
        }

        else -> if (circular) CircularProgressIndicator(modifier = modifier) else LinearProgressIndicator(modifier = modifier)
    }
}

@Preview
@Composable
private fun ProgressIndicatorCircularPreview() = Preview {
    Column(verticalArrangement = Arrangement.spacedBy(space = 8.dp)) {
        ProgressIndicator(circular = true)
        ProgressIndicator(progress = 0.0f, circular = true)
        ProgressIndicator(progress = 0.5f, circular = true)
        ProgressIndicator(progress = 1.0f, circular = true)
    }
}

@Preview
@Composable
private fun ProgressIndicatorLinearPreview() = Preview {
    Column(verticalArrangement = Arrangement.spacedBy(space = 8.dp)) {
        ProgressIndicator(circular = false)
        ProgressIndicator(progress = 0.0f, circular = false)
        ProgressIndicator(progress = 0.5f, circular = false)
        ProgressIndicator(progress = 1.0f, circular = false)
    }
}
