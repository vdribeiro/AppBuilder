package com.app.builder.ui

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.dp

/** The default shape for the smallest components. */
private val extraSmall = RoundedCornerShape(size = 4.0.dp)
/** The default shape for small components. */
private val small = RoundedCornerShape(size = 8.0.dp)
/** The default shape for medium-sized components. */
private val medium = RoundedCornerShape(size = 12.0.dp)
/** The default shape for large components. */
private val large = RoundedCornerShape(size = 16.0.dp)
/** The default shape for the largest components. */
private val extraLarge = RoundedCornerShape(size = 28.0.dp)

/** The default [Shapes], combining every shape defined above. */
private val shapes = Shapes(
    extraSmall = extraSmall,
    small = small,
    medium = medium,
    large = large,
    extraLarge = extraLarge
)

/** A CompositionLocal providing the current shapes. */
val LocalShapes: ProvidableCompositionLocal<Shapes> = staticCompositionLocalOf { shapes }
