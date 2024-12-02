package com.app.builder.ui.core.divider

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.app.builder.ui.Preview

/** Distinguishes between the structural axes a [Divider] can adopt. */
enum class Axis {
    /** Renders as a horizontal line, separating content stacked vertically. */
    HORIZONTAL,
    /** Renders as a vertical line, separating content arranged horizontally. */
    VERTICAL
}

/**
 * A layout separator component that provides a unified API for rendering line separators depending on the structural axis of the surrounding parent container layout.
 *
 * @param modifier The [Modifier] to be applied to the chosen divider implementation.
 * @param axis The structural axis to render. Defaults to [Axis.HORIZONTAL].
 */
@Composable
fun Divider(
    modifier: Modifier = Modifier,
    axis: Axis = Axis.HORIZONTAL,
) {
    when (axis) {
        Axis.HORIZONTAL -> HorizontalDivider(modifier = modifier)
        Axis.VERTICAL -> VerticalDivider(modifier = modifier)
    }
}

@Preview
@Composable
private fun DividerPreview() = Preview {
    Column {
        Divider(modifier = Modifier.padding(all = 4.dp), axis = Axis.HORIZONTAL)
        Divider(modifier = Modifier.padding(all = 4.dp), axis = Axis.VERTICAL)
    }
}
