package com.app.builder.ui.core.list

import kotlin.math.roundToInt
import kotlinx.coroutines.launch
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp

/**
 * The computed geometry of the scrollbar thumb at a given point in time.
 *
 * @property heightPx The height of the thumb, in pixels.
 * @property offsetPx The offset of the thumb from the top of the track, in pixels.
 * @property maxScrollPx The maximum amount the content can scroll by, in pixels.
 * @property maxOffsetPx The maximum offset the thumb can have within the track, in pixels.
 * @property reverseLayout Whether the underlying list is laid out in reverse.
 */
private class ThumbGeometry(
    val heightPx: Float,
    val offsetPx: Float,
    val maxScrollPx: Float,
    val maxOffsetPx: Float,
    val reverseLayout: Boolean
)

/**
 * Reads [LazyListState.scrollIndicatorState], which is annotated `@FrequentlyChangingValue` since it updates on every scroll frame.
 * Must only be called from a layout/draw phase or gesture callback, never directly from a composable body, or it will trigger a recomposition on every frame.
 */
private fun thumbGeometryOf(
    state: LazyListState,
    trackHeightPx: Float,
    minimalHeightPx: Float
): ThumbGeometry? {
    val indicator = state.scrollIndicatorState ?: return null
    val viewportHeightPx = indicator.viewportSize.toFloat()
    val contentHeightPx = indicator.contentSize.toFloat()
    val scrollOffsetPx = indicator.scrollOffset.toFloat()
    val maxScrollPx = contentHeightPx - viewportHeightPx
    if (viewportHeightPx <= 0f || contentHeightPx <= 0f || maxScrollPx <= 0f) return null

    val heightPx = (viewportHeightPx / contentHeightPx * trackHeightPx)
        .coerceIn(minimumValue = minimalHeightPx, maximumValue = trackHeightPx)
    val maxOffsetPx = trackHeightPx - heightPx

    val logicalOffsetPx = (scrollOffsetPx / maxScrollPx * maxOffsetPx)
        .coerceIn(minimumValue = 0f, maximumValue = maxOffsetPx)
    val reverseLayout = state.layoutInfo.reverseLayout
    val offsetPx = if (reverseLayout) maxOffsetPx - logicalOffsetPx else logicalOffsetPx

    return ThumbGeometry(heightPx, offsetPx, maxScrollPx, maxOffsetPx, reverseLayout)
}

@Composable
actual fun VerticalScrollBar(
    modifier: Modifier,
    state: LazyListState,
    minimalHeight: Dp,
    thickness: Dp,
    shape: Shape,
    hoverDurationMillis: Int,
    hoverColor: Color,
    unhoverColor: Color
) {
    val coroutineScope = rememberCoroutineScope()
    var isDragging by remember { mutableStateOf(value = false) }
    val thumbColor by animateColorAsState(
        targetValue = if (isDragging) hoverColor else unhoverColor,
        animationSpec = tween(durationMillis = hoverDurationMillis)
    )
    val density = LocalDensity.current
    val minimalHeightPx = with(density) { minimalHeight.toPx() }
    val thicknessPx = with(density) { thickness.roundToPx() }

    Box(
        modifier = modifier
            .layout { measurable, constraints ->
                val trackHeightPx = constraints.maxHeight
                val geometry = thumbGeometryOf(state, trackHeightPx.toFloat(), minimalHeightPx)
                val placeable = measurable.measure(
                    Constraints.fixed(
                        width = thicknessPx,
                        height = geometry?.heightPx?.roundToInt() ?: 0
                    )
                )
                layout(width = thicknessPx, height = trackHeightPx) {
                    placeable.placeRelative(x = 0, y = geometry?.offsetPx?.roundToInt() ?: 0)
                }
            }
            .clip(shape = shape)
            .background(color = thumbColor)
            .pointerInput(state, minimalHeightPx) {
                detectDragGestures(
                    onDragStart = { isDragging = true },
                    onDragEnd = { isDragging = false },
                    onDragCancel = { isDragging = false },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        val geometry = thumbGeometryOf(state, size.height.toFloat(), minimalHeightPx)
                            ?: return@detectDragGestures
                        if (geometry.maxOffsetPx > 0f) {
                            val logicalDragY = if (geometry.reverseLayout) -dragAmount.y else dragAmount.y
                            val scrollDelta = logicalDragY / geometry.maxOffsetPx * geometry.maxScrollPx
                            coroutineScope.launch { state.scrollBy(value = scrollDelta) }
                        }
                    }
                )
            }
    )
}
