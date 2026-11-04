package com.app.builder.ui.showcase

import kotlinx.collections.immutable.toPersistentList
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.app.builder.data.theme.CustomTheme
import com.app.builder.data.theme.ThemeCache
import com.app.builder.data.theme.ThemeMode
import com.app.builder.ui.ColorRole
import com.app.builder.ui.LocalColorScheme
import com.app.builder.ui.LocalShapes
import com.app.builder.ui.component.overlay.ColorPickerDialog
import com.app.builder.ui.core.button.ButtonGroup
import com.app.builder.ui.core.button.ButtonGroupItem
import com.app.builder.ui.core.text.Text
import com.app.builder.ui.isDarkTheme
import com.app.builder.ui.toHex

/**
 * Displays every color role of the current color scheme as a grid of labeled swatches, below a selector of the [ThemeMode].
 * Clicking a swatch opens a [ColorPickerDialog] that replaces the role's color in the [ThemeCache], for the color scheme currently in use.
 *
 * @param onThemeChange Invoked with the updated theme whenever the mode is changed or a color is applied or reset.
 */
@Composable
fun ColorsShowcase(onThemeChange: (CustomTheme) -> Unit = {}) {
    val colorScheme = LocalColorScheme.current
    val shapes = LocalShapes.current
    val customTheme by ThemeCache.flow.collectAsStateWithLifecycle()
    val dark = isDarkTheme()
    var editing by remember { mutableStateOf<ColorRole?>(value = null) }

    LazyVerticalGrid(
        modifier = Modifier.fillMaxSize().padding(all = 16.dp),
        columns = GridCells.Adaptive(minSize = 140.dp),
        horizontalArrangement = Arrangement.spacedBy(space = 12.dp),
        verticalArrangement = Arrangement.spacedBy(space = 12.dp)
    ) {
        item(span = { GridItemSpan(currentLineSpan = maxLineSpan) }) {
            ButtonGroup(
                modifier = Modifier.fillMaxWidth().testTag(tag = "theme_mode"),
                items = ThemeMode.entries.map { mode ->
                    ButtonGroupItem(
                        selected = mode == customTheme.mode,
                        text = mode.text(),
                        onClick = { onThemeChange(ThemeCache.setMode(mode = mode)) }
                    )
                }.toPersistentList()
            )
        }
        items(items = ColorRole.entries) { role ->
            val color = role.get(colorScheme = colorScheme)
            val onColor = role.contentRole()?.get(colorScheme = colorScheme) ?: Color.White
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(ratio = 1.6f)
                    .background(color = color, shape = shapes.small)
                    .clickable { editing = role }
                    .padding(all = 12.dp)
                    .testTag(tag = "color_swatch_${role.name}"),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Text(text = role.text, color = onColor)
                Text(text = color.toHex(), color = onColor)
            }
        }
    }

    editing?.let { role ->
        ColorPickerDialog(
            title = role.text,
            color = role.get(colorScheme = colorScheme),
            onConfirm = { color ->
                editing = null
                onThemeChange(ThemeCache.setColor(dark = dark, role = role.name, hex = color.toHex()))
            },
            onReset = {
                editing = null
                onThemeChange(ThemeCache.setColor(dark = dark, role = role.name, hex = null))
            },
            onDismiss = { editing = null }
        )
    }
}

/**
 * Resolves the label shown for a theme mode.
 *
 * @return The mode's label.
 */
private fun ThemeMode.text(): String = when (this) {
    ThemeMode.SYSTEM -> "System"
    ThemeMode.LIGHT -> "Light"
    ThemeMode.DARK -> "Dark"
}
