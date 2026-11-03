package com.app.builder.data.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import com.app.builder.ui.ColorRole
import com.app.builder.ui.ShapeRole

/**
 * Colors and shapes replacing the theme's default ones.
 *
 * @property light The colors replacing the light color scheme's, keyed by role.
 * @property dark The colors replacing the dark color scheme's, keyed by role.
 * @property shapes The corner radii replacing the shapes scale's, keyed by role.
 */
data class CustomTheme(
    val light: Map<ColorRole, Color> = emptyMap(),
    val dark: Map<ColorRole, Color> = emptyMap(),
    val shapes: Map<ShapeRole, Dp> = emptyMap(),
) {
    /**
     * Resolves the colors replacing one color scheme's.
     *
     * @param dark Whether to resolve the dark color scheme's colors.
     * @return The replacing colors, keyed by role.
     */
    fun colors(dark: Boolean): Map<ColorRole, Color> = if (dark) this.dark else light
}