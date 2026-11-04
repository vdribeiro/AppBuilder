package com.app.builder.ui

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.app.builder.data.theme.ThemeCache

/** A CompositionLocal providing the current shapes. */
val LocalShapes: ProvidableCompositionLocal<Shapes> = staticCompositionLocalOf { shapes() }

/**
 * Every customizable shape of the [Shapes] scale.
 *
 * @property text The label shown for this shape.
 * @property radius The default corner radius of this shape.
 */
enum class ShapeRole(val text: String, val radius: Dp) {
    /** The shape for the smallest components. */
    EXTRA_SMALL(text = "Extra small", radius = 4.dp),
    /** The shape for small components. */
    SMALL(text = "Small", radius = 8.dp),
    /** The shape for medium-sized components. */
    MEDIUM(text = "Medium", radius = 12.dp),
    /** The shape for large components. */
    LARGE(text = "Large", radius = 16.dp),
    /** The shape for the largest components. */
    EXTRA_LARGE(text = "Extra large", radius = 28.dp),
}

/**
 * Builds the [Shapes] scale, rounding each shape's corners by its replaced radius or its default one.
 *
 * @param radii The corner radii in dp replacing the default ones, keyed by shape role name.
 * @return The shapes scale.
 */
private fun shapes(radii: Map<String, Float> = emptyMap()): Shapes {
    fun shape(role: ShapeRole) = RoundedCornerShape(size = radii[role.name]?.dp ?: role.radius)
    return Shapes(
        extraSmall = shape(role = ShapeRole.EXTRA_SMALL),
        small = shape(role = ShapeRole.SMALL),
        medium = shape(role = ShapeRole.MEDIUM),
        large = shape(role = ShapeRole.LARGE),
        extraLarge = shape(role = ShapeRole.EXTRA_LARGE)
    )
}

/**
 * Resolves the current [Shapes], with the [ThemeCache] overrides applied.
 *
 * @return The shapes scale with its overrides applied.
 */
@Composable
internal fun getShapes(): Shapes {
    val customTheme by ThemeCache.flow.collectAsStateWithLifecycle()
    return remember(key1 = customTheme.shapes) { shapes(radii = customTheme.shapes) }
}
