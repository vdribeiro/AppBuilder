package com.app.builder.data.theme

import com.app.builder.core.flow.StateHolder

/** In-memory cache for the theme's color and shape. */
object ThemeCache: StateHolder<CustomTheme>(default = CustomTheme()) {

    /**
     * Replaces which color scheme the theme displays.
     *
     * @param mode The mode to apply.
     * @return The updated theme.
     */
    fun setMode(mode: ThemeMode): CustomTheme =
        set { theme -> theme.copy(mode = mode) }

    /**
     * Replaces a single role's color in one color scheme.
     *
     * @param dark Whether to change the dark color scheme.
     * @param role The name of the color role to change.
     * @param hex The hexadecimal color to assign, or `null` to restore the default one.
     * @return The updated theme.
     */
    fun setColor(dark: Boolean, role: String, hex: String?): CustomTheme =
        set { theme ->
            val colors = theme.colors(dark = dark).let { if (hex == null) it - role else it + (role to hex) }
            if (dark) theme.copy(dark = colors) else theme.copy(light = colors)
        }

    /**
     * Replaces a single shape's corner radius.
     *
     * @param role The name of the shape role to change.
     * @param radius The corner radius in dp to assign, or `null` to restore the default one.
     * @return The updated theme.
     */
    fun setShape(role: String, radius: Float?): CustomTheme =
        set { theme -> theme.copy(shapes = if (radius == null) theme.shapes - role else theme.shapes + (role to radius)) }
}
