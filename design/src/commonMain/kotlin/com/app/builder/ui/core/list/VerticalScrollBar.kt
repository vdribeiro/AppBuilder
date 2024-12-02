package com.app.builder.ui.core.list

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp

/**
 * A platform-specific layout component representing a vertical scrollbar thumb tracker.
 * This component maps structural scrolling viewport coordinates from a [LazyListState] and renders a custom scroll indicator.
 *
 * @param modifier The [Modifier] to apply to the scrollbar layout container.
 * @param state The reactive [LazyListState] bound to the companion lazy collection layout.
 * @param minimalHeight The structural safety floor constraint preventing the scroll thumb from shrinking past this height.
 * @param thickness The width dimension thickness of the scrollbar tracks.
 * @param shape The geometric rounding curve applied to the outer clip of the indicator thumb.
 * @param hoverDurationMillis The duration in milliseconds for blending transitions during cursor focus shifts.
 * @param hoverColor The paint color signature rendered when the pointer actively hovers over the indicator track.
 * @param unhoverColor The passive rest color state signature rendered when no pointer interaction is detected.
 */
@Composable
expect fun VerticalScrollBar(
    modifier: Modifier = Modifier,
    state: LazyListState,
    minimalHeight: Dp,
    thickness: Dp,
    shape: Shape,
    hoverDurationMillis: Int,
    hoverColor: Color,
    unhoverColor: Color
)
