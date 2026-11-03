package com.app.builder.data.theme

import kotlinx.serialization.Serializable

/**
 * Colors and shapes replacing the theme's default ones.
 *
 * @property mode Which color scheme the theme displays.
 * @property light Hexadecimal colors replacing the light color scheme's, keyed by color role name.
 * @property dark Hexadecimal colors replacing the dark color scheme's, keyed by color role name.
 * @property shapes Corner radii in dp replacing the shapes scale's, keyed by shape role name.
 */
@Serializable
data class CustomTheme(
    val mode: ThemeMode = ThemeMode.SYSTEM,
    val light: Map<String, String> = emptyMap(),
    val dark: Map<String, String> = emptyMap(),
    val shapes: Map<String, Float> = emptyMap(),
) {
    /**
     * Resolves the colors replacing one color scheme's.
     *
     * @param dark Whether to resolve the dark color scheme's colors.
     * @return The replacing hexadecimal colors, keyed by color role name.
     */
    fun colors(dark: Boolean): Map<String, String> = if (dark) this.dark else light
}
