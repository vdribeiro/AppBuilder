package com.app.builder.ui.core.overlay

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.app.builder.ui.LocalTypography
import com.app.builder.ui.Preview
import com.app.builder.ui.core.image.Icon
import com.app.builder.ui.core.text.Text
import androidx.compose.material3.Badge as MaterialBadge
import androidx.compose.material3.BadgedBox as MaterialBadgedBox

/**
 * A small status descriptor drawing attention to an unread count or unseen event, rendering either a minimal dot or a short text label.
 *
 * @param modifier The [Modifier] to be applied to the badge's layout.
 * @param text An optional short label rendered inside the badge. If null, a minimal dot indicator is drawn instead.
 */
@Composable
fun Badge(
    modifier: Modifier = Modifier,
    text: String? = null,
) {
    val typography = LocalTypography.current

    MaterialBadge(
        modifier = modifier,
        content = text?.let {
            {
                Text(
                    text = it,
                    maxLines = 1,
                    style = typography.labelSmall
                )
            }
        }
    )
}

/**
 * Anchors a [badge] to the top-end corner of the wrapped [content], commonly used to flag unread counts on icons or navigation destinations.
 *
 * @param modifier The [Modifier] to be applied to the anchored layout.
 * @param badge A composable slot rendering the badge indicator, typically a [Badge].
 * @param content The anchor composable content, typically an [Icon].
 */
@Composable
fun BadgedBox(
    modifier: Modifier = Modifier,
    badge: @Composable BoxScope.() -> Unit = { Badge() },
    content: @Composable BoxScope.() -> Unit = {},
) {
    MaterialBadgedBox(
        modifier = modifier,
        badge = badge,
        content = content
    )
}

@Preview
@Composable
private fun BadgePreview() = Preview {
    Column(verticalArrangement = Arrangement.spacedBy(space = 8.dp)) {
        BadgedBox(badge = { Badge() }) { Icon(imageVector = Icons.Default.Apps) }
        BadgedBox(badge = { Badge(text = "3") }) { Icon(imageVector = Icons.Default.Apps) }
        BadgedBox(badge = { Badge(text = "99+") }) { Icon(imageVector = Icons.Default.Apps) }
        BadgedBox(badge = { Badge(text = "9000") }) { Icon(imageVector = Icons.Default.Apps) }
    }
}
