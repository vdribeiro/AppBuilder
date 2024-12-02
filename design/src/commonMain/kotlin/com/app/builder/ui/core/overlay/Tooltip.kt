package com.app.builder.ui.core.overlay

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.TooltipAnchorPosition
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.app.builder.ui.Preview
import com.app.builder.ui.core.button.Button
import com.app.builder.ui.core.image.Icon
import com.app.builder.ui.core.text.Text

/** Enum representing the position of the anchor relative to the tooltip. */
enum class Position {
    /** Positions the tooltip above the anchor. */
    ABOVE,
    /** Positions the tooltip below the anchor. */
    BELOW,
    /** Positions the tooltip to the left of the anchor, regardless of layout direction. */
    LEFT,
    /** Positions the tooltip to the right of the anchor, regardless of layout direction. */
    RIGHT,
    /** Positions the tooltip before the anchor, respecting the current layout direction. */
    START,
    /** Positions the tooltip after the anchor, respecting the current layout direction. */
    END
}

/**
 * Wraps an anchor composable with a short descriptive label that appears on long-press or hover.
 *
 * @param modifier The [Modifier] to be applied to the tooltip bubble's layout.
 * @param text The short descriptive message to display when the tooltip is shown.
 * @param position The position of the anchor relative to the tooltip. Defaults to [Position.ABOVE].
 * @param content The anchor composable the tooltip attaches to, typically an [Icon] or [Button].
 */
@Composable
fun Tooltip(
    modifier: Modifier = Modifier,
    text: String? = null,
    position: Position = Position.ABOVE,
    content: @Composable () -> Unit = {},
) {
    val positioning = when (position) {
        Position.ABOVE -> TooltipAnchorPosition.Above
        Position.BELOW -> TooltipAnchorPosition.Below
        Position.LEFT -> TooltipAnchorPosition.Left
        Position.RIGHT -> TooltipAnchorPosition.Right
        Position.START -> TooltipAnchorPosition.Start
        Position.END -> TooltipAnchorPosition.End
    }
    TooltipBox(
        positionProvider = TooltipDefaults.rememberTooltipPositionProvider(positioning = positioning),
        tooltip = {
            PlainTooltip(modifier = modifier) {
                Text(text = text.orEmpty())
            }
        },
        state = rememberTooltipState(),
        enableUserInput = true,
        content = content
    )
}

@Preview
@Composable
private fun TooltipPreview() = Preview {
    Column(verticalArrangement = Arrangement.spacedBy(space = 8.dp)) {
        Position.entries.forEach {
            Tooltip(text = it.name, position = it) {
                Button(content = { Icon(imageVector = Icons.Default.Apps) })
            }
        }
    }
}
