package com.app.builder.ui.core.list

import androidx.compose.foundation.LocalScrollbarStyle
import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp

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
    val currentStyle = LocalScrollbarStyle.current
    VerticalScrollbar(
        modifier = modifier,
        adapter = rememberScrollbarAdapter(scrollState = state),
        style = currentStyle.copy(
            minimalHeight = minimalHeight,
            thickness = thickness,
            shape = shape,
            hoverDurationMillis = hoverDurationMillis,
            hoverColor = hoverColor,
            unhoverColor = unhoverColor
        )
    )
}
