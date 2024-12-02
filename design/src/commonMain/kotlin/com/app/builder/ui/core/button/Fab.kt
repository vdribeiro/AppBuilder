package com.app.builder.ui.core.button

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.LargeFloatingActionButton
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.app.builder.ui.LocalColorScheme
import com.app.builder.ui.Preview
import com.app.builder.ui.core.image.Icon

/** The container sizes a [Fab] can be rendered at. */
enum class FabSize {
    /** A compact container, for use when screen space is limited. */
    SMALL,
    /** The standard container size. */
    REGULAR,
    /** An enlarged container, for use on larger screens. */
    LARGE
}

/**
 * A floating action button representing the single most important action on a screen.
 *
 * @param modifier The [Modifier] to be applied to the button layout.
 * @param size The container size to render. Defaults to [FabSize.REGULAR].
 * @param color The container color treatment to render.
 * @param onClick Will be called when the user clicks the button.
 * @param content The composable content to be rendered inside the button.
 */
@Composable
fun Fab(
    modifier: Modifier = Modifier,
    size: FabSize = FabSize.REGULAR,
    color: Color = LocalColorScheme.current.primaryContainer,
    onClick: () -> Unit = {},
    content: @Composable () -> Unit = {},
) {
    val contentColor = contentColorFor(backgroundColor = color)
    when (size) {
        FabSize.SMALL -> SmallFloatingActionButton(modifier = modifier, onClick = onClick, containerColor = color, contentColor = contentColor, content = content)
        FabSize.REGULAR -> FloatingActionButton(modifier = modifier, onClick = onClick, containerColor = color, contentColor = contentColor, content = content)
        FabSize.LARGE -> LargeFloatingActionButton(modifier = modifier, onClick = onClick, containerColor = color, contentColor = contentColor, content = content)
    }
}

// TODO - FAB menu

@Preview
@Composable
private fun FabPreview() = Preview {
    Column(verticalArrangement = Arrangement.spacedBy(space = 8.dp)) {
        Fab(size = FabSize.SMALL) { Icon(imageVector = Icons.Default.Add) }
        Fab(size = FabSize.REGULAR) { Icon(imageVector = Icons.Default.Add) }
        Fab(size = FabSize.LARGE) { Icon(imageVector = Icons.Default.Add) }
    }
}
