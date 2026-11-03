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

/** In-memory cache for the theme's color and shape overrides. */
object ThemeCache {

    /** Backing mutable state flow holding the current overrides. */
    private val _cacheState = MutableStateFlow(value = CustomTheme())
    /** Read-only state flow emitting the current overrides. */
    val cacheState: StateFlow<CustomTheme> = _cacheState.asStateFlow()

    /**
     * Replaces the current overrides.
     *
     * @param overrides The new overrides to apply.
     */
    fun set(overrides: CustomTheme) =
        _cacheState.update { overrides }

    /**
     * Replaces a single role's color in one color scheme.
     *
     * @param dark Whether to change the dark color scheme.
     * @param role The role to change.
     * @param color The color to assign, or `null` to restore the default one.
     * @return The updated overrides.
     */
    fun setColor(dark: Boolean, role: ColorRole, color: Color?): CustomTheme =
        _cacheState.updateAndGet { overrides ->
            val colors = overrides.colors(dark = dark).let { if (color == null) it - role else it + (role to color) }
            if (dark) overrides.copy(dark = colors) else overrides.copy(light = colors)
        }

    /**
     * Replaces a single shape's corner radius.
     *
     * @param role The shape to change.
     * @param radius The corner radius to assign, or `null` to restore the default one.
     * @return The updated overrides.
     */
    fun setShape(role: ShapeRole, radius: Dp?): CustomTheme =
        _cacheState.updateAndGet { overrides ->
            overrides.copy(shapes = if (radius == null) overrides.shapes - role else overrides.shapes + (role to radius))
        }
}
