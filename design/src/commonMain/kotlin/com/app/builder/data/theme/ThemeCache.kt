package com.app.builder.data.theme

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.updateAndGet
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import com.app.builder.ui.ColorRole
import com.app.builder.ui.ShapeRole

/** In-memory cache for the theme's color and shape. */
object ThemeCache {

    /** Backing mutable state flow holding the current theme. */
    private val _cacheState = MutableStateFlow(value = CustomTheme())
    /** Read-only state flow emitting the current theme. */
    val cacheState: StateFlow<CustomTheme> = _cacheState.asStateFlow()

    /**
     * Replaces the current theme.
     *
     * @param customTheme The new theme to apply.
     */
    fun set(customTheme: CustomTheme) =
        _cacheState.update { customTheme }

    /**
     * Replaces a single role's color in one color scheme.
     *
     * @param dark Whether to change the dark color scheme.
     * @param role The role to change.
     * @param color The color to assign, or `null` to restore the default one.
     * @return The updated theme.
     */
    fun setColor(dark: Boolean, role: ColorRole, color: Color?): CustomTheme =
        _cacheState.updateAndGet { theme ->
            val colors = theme.colors(dark = dark).let { if (color == null) it - role else it + (role to color) }
            if (dark) theme.copy(dark = colors) else theme.copy(light = colors)
        }

    /**
     * Replaces a single shape's corner radius.
     *
     * @param role The shape to change.
     * @param radius The corner radius to assign, or `null` to restore the default one.
     * @return The updated theme.
     */
    fun setShape(role: ShapeRole, radius: Dp?): CustomTheme =
        _cacheState.updateAndGet { theme ->
            theme.copy(shapes = if (radius == null) theme.shapes - role else theme.shapes + (role to radius))
        }
}
