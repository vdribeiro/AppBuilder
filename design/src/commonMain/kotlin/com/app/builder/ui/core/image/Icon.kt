package com.app.builder.ui.core.image

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.app.builder.ui.LocalColorScheme
import com.app.builder.ui.Preview
import com.app.builder.ui.getTranslation
import androidx.compose.material3.Icon as MaterialIcon

/**
 * Asset renderer.
 * If an [imageVector] is supplied, it displays the specified vector graphic with the given [tint].
 * If the [imageVector] is null, it renders an invisible structural spacer container using [emptySize] as padding to maintain expected layout boundaries and alignment metrics.
 *
 * @param modifier The [Modifier] to be applied to either the rendered icon layout or the fallback box structure.
 * @param imageVector The vector graphic asset to draw. If null, the component falls back to rendering empty space.
 * @param tint The color theme applied to the vector artwork layer. Defaults to [LocalContentColor].
 * @param contentDescription Localized accessibility description text. Can be null if purely decorative.
 * @param emptySize The padding layout size applied to all edges of the fallback container when [imageVector] is null. Defaults to 16.dp.
 */
@Composable
fun Icon(
    modifier: Modifier = Modifier,
    imageVector: ImageVector? = null,
    tint: Color = LocalContentColor.current,
    contentDescription: String? = null,
    emptySize: Dp = 16.dp
) {
    if (imageVector != null) MaterialIcon(
        modifier = modifier,
        imageVector = imageVector,
        tint = tint,
        contentDescription = contentDescription?.let { getTranslation(key = it) }
    ) else Box(modifier = Modifier.padding(all = emptySize))
}

@Preview
@Composable
private fun IconPreview() = Preview {
    Column(verticalArrangement = Arrangement.spacedBy(space = 8.dp)) {
        val colorScheme = LocalColorScheme.current
        Icon(imageVector = Icons.Default.Apps, tint = colorScheme.primary)
        Icon()
        Icon(imageVector = Icons.Default.Apps)
    }
}
