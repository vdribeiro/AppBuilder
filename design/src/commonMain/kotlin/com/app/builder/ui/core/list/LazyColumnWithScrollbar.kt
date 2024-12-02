package com.app.builder.ui.core.list

import androidx.compose.foundation.gestures.ScrollableDefaults
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberOverscrollEffect
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.app.builder.core.config.ClientFlags
import com.app.builder.ui.LocalShapes
import com.app.builder.ui.Preview
import com.app.builder.ui.core.card.Card
import com.app.builder.ui.core.image.Icon
import com.app.builder.ui.core.text.Text

/**
 * A layout list component that encapsulates a standard vertical layout stream with an integrated, optional, reactive [VerticalScrollBar].
 * The scrollbar overlay tracks layout coordinates dynamically and updates visibility and animations natively depending on cursor/pointer hover states.
 *
 * @param modifier The [Modifier] to apply to the root [Box] stacking layout container.
 * @param state The state object controlling and observing scroll lifecycle events for the lazy collection.
 * @param scrollBar When `true`, initializes and pins a [VerticalScrollBar] alignment overlay to the container's end edge.
 * @param contentPadding Outer bounding padding offsets injected surrounding the totality of list stream items.
 * @param reverseLayout When `true`, reverses the layout rendering sequence and scroll layout direction.
 * @param verticalArrangement Vertical layout alignment strategy specifying cross-axis gaps between item rows.
 * @param horizontalAlignment Horizontal alignment constraint applied to child layouts within the column scope.
 * @param userScrollEnabled When `false`, manual mechanical gesture scrolling triggers are rejected.
 * @param scrollBarMinimalHeight Bound constraint preventing the interactive scroll handle from collapsing below a set size.
 * @param scrollBarThickness Transverse thickness dimension of the scrolling timeline indicator bar.
 * @param scrollBarShape Geometric clip mapping applied to the exterior border bounds of the scroll indicator thumb.
 * @param scrollBarHoverDurationMillis Fade animation transition window measuring adjustments between active hover indicators.
 * @param scrollBarHoverColor Color asset signature representing the indicator thumb when pointer hover focus is acquired.
 * @param scrollBarUnhoverColor Rest color signature mapping representing the indicator thumb in standard passive states.
 * @param content The domain-scoped construction DSL lambda injecting standard list cell compositions.
 */
@Composable
fun LazyColumn(
    modifier: Modifier = Modifier,
    state: LazyListState = rememberLazyListState(),
    scrollBar: Boolean = ClientFlags.flags.scrollBar,
    contentPadding: PaddingValues = PaddingValues(all = 0.dp),
    reverseLayout: Boolean = false,
    verticalArrangement: Arrangement.Vertical = if (!reverseLayout) Arrangement.Top else Arrangement.Bottom,
    horizontalAlignment: Alignment.Horizontal = Alignment.Start,
    userScrollEnabled: Boolean = true,
    scrollBarMinimalHeight: Dp = 64.dp,
    scrollBarThickness: Dp = 8.dp,
    scrollBarShape: Shape = LocalShapes.current.extraSmall,
    scrollBarHoverDurationMillis: Int = 300,
    scrollBarHoverColor: Color = LocalContentColor.current,
    scrollBarUnhoverColor: Color = scrollBarHoverColor.copy(alpha = 0.3f),
    content: LazyListScope.() -> Unit = {}
) {
    Box(modifier = modifier) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            state = state,
            contentPadding = contentPadding,
            reverseLayout = reverseLayout,
            verticalArrangement = verticalArrangement,
            horizontalAlignment = horizontalAlignment,
            flingBehavior = ScrollableDefaults.flingBehavior(),
            userScrollEnabled = userScrollEnabled,
            overscrollEffect = rememberOverscrollEffect(),
            content = content
        )
        if (scrollBar) VerticalScrollBar(
            modifier = Modifier
                .padding(all = 4.dp)
                .fillMaxHeight()
                .align(Alignment.CenterEnd),
            state = state,
            minimalHeight = scrollBarMinimalHeight,
            thickness = scrollBarThickness,
            shape = scrollBarShape,
            hoverDurationMillis = scrollBarHoverDurationMillis,
            hoverColor = scrollBarHoverColor,
            unhoverColor = scrollBarUnhoverColor
        )
    }
}

@Preview
@Composable
private fun LazyColumnPreview() = Preview {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(all = 16.dp),
        verticalArrangement = Arrangement.spacedBy(space = 16.dp),
        scrollBar = true
    ) {
        item {
            ListItem(
                headlineText = "Headline",
                supportingText = "Supporting text",
                leadingContent = { Icon(imageVector = Icons.Default.Apps) },
            )
        }
        item {
            Card(
                content = {
                    Text(
                        modifier = Modifier.padding(all = 16.dp),
                        text = "Item"
                    )
                }
            )
        }
    }
}
