package com.app.builder.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.app.builder.ui.core.text.Text

/** A labeled color role paired with its contrasting "on" color, used to render a swatch. */
private data class ColorSwatch(val label: String, val color: Color, val onColor: Color)

/** Displays every color role of the current color scheme as a grid of labeled swatches. */
@Composable
fun ColorsShowcase() {
    val colorScheme = LocalColorScheme.current
    val shapes = LocalShapes.current
    val swatches = listOf(
        ColorSwatch(label = "Primary", color = colorScheme.primary, onColor = colorScheme.onPrimary),
        ColorSwatch(label = "Primary container", color = colorScheme.primaryContainer, onColor = colorScheme.onPrimaryContainer),
        ColorSwatch(label = "Secondary", color = colorScheme.secondary, onColor = colorScheme.onSecondary),
        ColorSwatch(label = "Secondary container", color = colorScheme.secondaryContainer, onColor = colorScheme.onSecondaryContainer),
        ColorSwatch(label = "Tertiary", color = colorScheme.tertiary, onColor = colorScheme.onTertiary),
        ColorSwatch(label = "Tertiary container", color = colorScheme.tertiaryContainer, onColor = colorScheme.onTertiaryContainer),
        ColorSwatch(label = "Error", color = colorScheme.error, onColor = colorScheme.onError),
        ColorSwatch(label = "Error container", color = colorScheme.errorContainer, onColor = colorScheme.onErrorContainer),
        ColorSwatch(label = "Background", color = colorScheme.background, onColor = colorScheme.onBackground),
        ColorSwatch(label = "Surface", color = colorScheme.surface, onColor = colorScheme.onSurface),
        ColorSwatch(label = "Surface variant", color = colorScheme.surfaceVariant, onColor = colorScheme.onSurfaceVariant),
        ColorSwatch(label = "Surface dim", color = colorScheme.surfaceDim, onColor = colorScheme.onSurface),
        ColorSwatch(label = "Surface bright", color = colorScheme.surfaceBright, onColor = colorScheme.onSurface),
        ColorSwatch(label = "Surface container lowest", color = colorScheme.surfaceContainerLowest, onColor = colorScheme.onSurface),
        ColorSwatch(label = "Surface container low", color = colorScheme.surfaceContainerLow, onColor = colorScheme.onSurface),
        ColorSwatch(label = "Surface container", color = colorScheme.surfaceContainer, onColor = colorScheme.onSurface),
        ColorSwatch(label = "Surface container high", color = colorScheme.surfaceContainerHigh, onColor = colorScheme.onSurface),
        ColorSwatch(label = "Surface container highest", color = colorScheme.surfaceContainerHighest, onColor = colorScheme.onSurface),
        ColorSwatch(label = "Inverse surface", color = colorScheme.inverseSurface, onColor = colorScheme.inverseOnSurface),
        ColorSwatch(label = "Inverse primary", color = colorScheme.inversePrimary, onColor = colorScheme.onPrimary),
        ColorSwatch(label = "Outline", color = colorScheme.outline, onColor = colorScheme.background),
        ColorSwatch(label = "Outline variant", color = colorScheme.outlineVariant, onColor = colorScheme.onSurface),
        ColorSwatch(label = "Scrim", color = colorScheme.scrim, onColor = Color.White),
    )

    LazyVerticalGrid(
        modifier = Modifier.fillMaxSize().padding(all = 16.dp),
        columns = GridCells.Adaptive(minSize = 140.dp),
        horizontalArrangement = Arrangement.spacedBy(space = 12.dp),
        verticalArrangement = Arrangement.spacedBy(space = 12.dp)
    ) {
        items(items = swatches) { swatch ->
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(ratio = 1.6f)
                    .background(color = swatch.color, shape = shapes.small)
                    .padding(all = 12.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Text(text = swatch.label, color = swatch.onColor)
                Text(text = swatch.color.toHex(), color = swatch.onColor)
            }
        }
    }
}

/** Converts a color to its hexadecimal representation. */
private fun Color.toHex(): String {
    fun Float.toByteHex() = (this * 255f).toInt().coerceIn(minimumValue = 0, maximumValue = 255)
        .toString(radix = 16).padStart(length = 2, padChar = '0')
    return "#${red.toByteHex()}${green.toByteHex()}${blue.toByteHex()}".uppercase()
}
