package com.app.builder.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.app.builder.data.theme.ThemeCache

/** The primary color for the light color scheme. */
internal val primaryLight = Color(0xFF36618E)
/** The on primary color for the light color scheme. */
internal val onPrimaryLight = Color(0xFFFFFFFF)
/** The primary container color for the light color scheme. */
internal val primaryContainerLight = Color(0xFFD1E4FF)
/** The on primary container color for the light color scheme. */
internal val onPrimaryContainerLight = Color(0xFF1A4975)
/** The secondary color for the light color scheme. */
internal val secondaryLight = Color(0xFF535F70)
/** The on secondary color for the light color scheme. */
internal val onSecondaryLight = Color(0xFFFFFFFF)
/** The secondary container color for the light color scheme. */
internal val secondaryContainerLight = Color(0xFFD7E3F8)
/** The on secondary container color for the light color scheme. */
internal val onSecondaryContainerLight = Color(0xFF3B4858)
/** The tertiary color for the light color scheme. */
internal val tertiaryLight = Color(0xFF6B5778)
/** The on tertiary color for the light color scheme. */
internal val onTertiaryLight = Color(0xFFFFFFFF)
/** The tertiary container color for the light color scheme. */
internal val tertiaryContainerLight = Color(0xFFF3DAFF)
/** The on tertiary container color for the light color scheme. */
internal val onTertiaryContainerLight = Color(0xFF523F5F)
/** The error color for the light color scheme. */
internal val errorLight = Color(0xFFBA1A1A)
/** The on error color for the light color scheme. */
internal val onErrorLight = Color(0xFFFFFFFF)
/** The error container color for the light color scheme. */
internal val errorContainerLight = Color(0xFFFFDAD6)
/** The on error container color for the light color scheme. */
internal val onErrorContainerLight = Color(0xFF93000A)
/** The background color for the light color scheme. */
internal val backgroundLight = Color(0xFFF8F9FF)
/** The on background color for the light color scheme. */
internal val onBackgroundLight = Color(0xFF191C20)
/** The surface color for the light color scheme. */
internal val surfaceLight = Color(0xFFF8F9FF)
/** The on surface color for the light color scheme. */
internal val onSurfaceLight = Color(0xFF191C20)
/** The surface variant color for the light color scheme. */
internal val surfaceVariantLight = Color(0xFFDFE2EB)
/** The on surface variant color for the light color scheme. */
internal val onSurfaceVariantLight = Color(0xFF43474E)
/** The outline color for the light color scheme. */
internal val outlineLight = Color(0xFF73777F)
/** The outline variant color for the light color scheme. */
internal val outlineVariantLight = Color(0xFFC3C6CF)
/** The scrim color for the light color scheme. */
internal val scrimLight = Color(0xFF000000)
/** The inverse surface color for the light color scheme. */
internal val inverseSurfaceLight = Color(0xFF2E3135)
/** The inverse on surface color for the light color scheme. */
internal val inverseOnSurfaceLight = Color(0xFFEFF0F7)
/** The inverse primary color for the light color scheme. */
internal val inversePrimaryLight = Color(0xFFA0CAFD)
/** The surface dim color for the light color scheme. */
internal val surfaceDimLight = Color(0xFFD8DAE0)
/** The surface bright color for the light color scheme. */
internal val surfaceBrightLight = Color(0xFFF8F9FF)
/** The surface container lowest color for the light color scheme. */
internal val surfaceContainerLowestLight = Color(0xFFFFFFFF)
/** The surface container low color for the light color scheme. */
internal val surfaceContainerLowLight = Color(0xFFF2F3FA)
/** The surface container color for the light color scheme. */
internal val surfaceContainerLight = Color(0xFFECEEF4)
/** The surface container high color for the light color scheme. */
internal val surfaceContainerHighLight = Color(0xFFE6E8EE)
/** The surface container highest color for the light color scheme. */
internal val surfaceContainerHighestLight = Color(0xFFE1E2E8)

/** The primary color for the dark color scheme. */
internal val primaryDark = Color(0xFFA0CAFD)
/** The on primary color for the dark color scheme. */
internal val onPrimaryDark = Color(0xFF003258)
/** The primary container color for the dark color scheme. */
internal val primaryContainerDark = Color(0xFF1A4975)
/** The on primary container color for the dark color scheme. */
internal val onPrimaryContainerDark = Color(0xFFD1E4FF)
/** The secondary color for the dark color scheme. */
internal val secondaryDark = Color(0xFFBBC7DB)
/** The on secondary color for the dark color scheme. */
internal val onSecondaryDark = Color(0xFF253140)
/** The secondary container color for the dark color scheme. */
internal val secondaryContainerDark = Color(0xFF3B4858)
/** The on secondary container color for the dark color scheme. */
internal val onSecondaryContainerDark = Color(0xFFD7E3F8)
/** The tertiary color for the dark color scheme. */
internal val tertiaryDark = Color(0xFFD7BEE4)
/** The on tertiary color for the dark color scheme. */
internal val onTertiaryDark = Color(0xFF3B2948)
/** The tertiary container color for the dark color scheme. */
internal val tertiaryContainerDark = Color(0xFF523F5F)
/** The on tertiary container color for the dark color scheme. */
internal val onTertiaryContainerDark = Color(0xFFF3DAFF)
/** The error color for the dark color scheme. */
internal val errorDark = Color(0xFFFFB4AB)
/** The on error color for the dark color scheme. */
internal val onErrorDark = Color(0xFF690005)
/** The error container color for the dark color scheme. */
internal val errorContainerDark = Color(0xFF93000A)
/** The on error container color for the dark color scheme. */
internal val onErrorContainerDark = Color(0xFFFFDAD6)
/** The background color for the dark color scheme. */
internal val backgroundDark = Color(0xFF111418)
/** The on background color for the dark color scheme. */
internal val onBackgroundDark = Color(0xFFE1E2E8)
/** The surface color for the dark color scheme. */
internal val surfaceDark = Color(0xFF111418)
/** The on surface color for the dark color scheme. */
internal val onSurfaceDark = Color(0xFFE1E2E8)
/** The surface variant color for the dark color scheme. */
internal val surfaceVariantDark = Color(0xFF43474E)
/** The on surface variant color for the dark color scheme. */
internal val onSurfaceVariantDark = Color(0xFFC3C6CF)
/** The outline color for the dark color scheme. */
internal val outlineDark = Color(0xFF8D9199)
/** The outline variant color for the dark color scheme. */
internal val outlineVariantDark = Color(0xFF43474E)
/** The scrim color for the dark color scheme. */
internal val scrimDark = Color(0xFF000000)
/** The inverse surface color for the dark color scheme. */
internal val inverseSurfaceDark = Color(0xFFE1E2E8)
/** The inverse on surface color for the dark color scheme. */
internal val inverseOnSurfaceDark = Color(0xFF2E3135)
/** The inverse primary color for the dark color scheme. */
internal val inversePrimaryDark = Color(0xFF36618E)
/** The surface dim color for the dark color scheme. */
internal val surfaceDimDark = Color(0xFF111418)
/** The surface bright color for the dark color scheme. */
internal val surfaceBrightDark = Color(0xFF36393E)
/** The surface container lowest color for the dark color scheme. */
internal val surfaceContainerLowestDark = Color(0xFF0B0E13)
/** The surface container low color for the dark color scheme. */
internal val surfaceContainerLowDark = Color(0xFF191C20)
/** The surface container color for the dark color scheme. */
internal val surfaceContainerDark = Color(0xFF1D2024)
/** The surface container high color for the dark color scheme. */
internal val surfaceContainerHighDark = Color(0xFF272A2F)
/** The surface container highest color for the dark color scheme. */
internal val surfaceContainerHighestDark = Color(0xFF32353A)

/** The [ColorScheme] used when the app is displayed with a light theme. */
internal val lightColorScheme = lightColorScheme(
    primary = primaryLight,
    onPrimary = onPrimaryLight,
    primaryContainer = primaryContainerLight,
    onPrimaryContainer = onPrimaryContainerLight,
    secondary = secondaryLight,
    onSecondary = onSecondaryLight,
    secondaryContainer = secondaryContainerLight,
    onSecondaryContainer = onSecondaryContainerLight,
    tertiary = tertiaryLight,
    onTertiary = onTertiaryLight,
    tertiaryContainer = tertiaryContainerLight,
    onTertiaryContainer = onTertiaryContainerLight,
    error = errorLight,
    onError = onErrorLight,
    errorContainer = errorContainerLight,
    onErrorContainer = onErrorContainerLight,
    background = backgroundLight,
    onBackground = onBackgroundLight,
    surface = surfaceLight,
    onSurface = onSurfaceLight,
    surfaceVariant = surfaceVariantLight,
    onSurfaceVariant = onSurfaceVariantLight,
    outline = outlineLight,
    outlineVariant = outlineVariantLight,
    scrim = scrimLight,
    inverseSurface = inverseSurfaceLight,
    inverseOnSurface = inverseOnSurfaceLight,
    inversePrimary = inversePrimaryLight,
    surfaceDim = surfaceDimLight,
    surfaceBright = surfaceBrightLight,
    surfaceContainerLowest = surfaceContainerLowestLight,
    surfaceContainerLow = surfaceContainerLowLight,
    surfaceContainer = surfaceContainerLight,
    surfaceContainerHigh = surfaceContainerHighLight,
    surfaceContainerHighest = surfaceContainerHighestLight,
)

/** The [ColorScheme] used when the app is displayed with a dark theme. */
internal val darkColorScheme = darkColorScheme(
    primary = primaryDark,
    onPrimary = onPrimaryDark,
    primaryContainer = primaryContainerDark,
    onPrimaryContainer = onPrimaryContainerDark,
    secondary = secondaryDark,
    onSecondary = onSecondaryDark,
    secondaryContainer = secondaryContainerDark,
    onSecondaryContainer = onSecondaryContainerDark,
    tertiary = tertiaryDark,
    onTertiary = onTertiaryDark,
    tertiaryContainer = tertiaryContainerDark,
    onTertiaryContainer = onTertiaryContainerDark,
    error = errorDark,
    onError = onErrorDark,
    errorContainer = errorContainerDark,
    onErrorContainer = onErrorContainerDark,
    background = backgroundDark,
    onBackground = onBackgroundDark,
    surface = surfaceDark,
    onSurface = onSurfaceDark,
    surfaceVariant = surfaceVariantDark,
    onSurfaceVariant = onSurfaceVariantDark,
    outline = outlineDark,
    outlineVariant = outlineVariantDark,
    scrim = scrimDark,
    inverseSurface = inverseSurfaceDark,
    inverseOnSurface = inverseOnSurfaceDark,
    inversePrimary = inversePrimaryDark,
    surfaceDim = surfaceDimDark,
    surfaceBright = surfaceBrightDark,
    surfaceContainerLowest = surfaceContainerLowestDark,
    surfaceContainerLow = surfaceContainerLowDark,
    surfaceContainer = surfaceContainerDark,
    surfaceContainerHigh = surfaceContainerHighDark,
    surfaceContainerHighest = surfaceContainerHighestDark,
)

/** A CompositionLocal providing the current color scheme in use. */
val LocalColorScheme = staticCompositionLocalOf { lightColorScheme }

/**
 * Every customizable color role of a [ColorScheme].
 *
 * @property text The label shown for this role.
 */
enum class ColorRole(val text: String) {
    PRIMARY(text = "Primary"),
    ON_PRIMARY(text = "On primary"),
    PRIMARY_CONTAINER(text = "Primary container"),
    ON_PRIMARY_CONTAINER(text = "On primary container"),
    SECONDARY(text = "Secondary"),
    ON_SECONDARY(text = "On secondary"),
    SECONDARY_CONTAINER(text = "Secondary container"),
    ON_SECONDARY_CONTAINER(text = "On secondary container"),
    TERTIARY(text = "Tertiary"),
    ON_TERTIARY(text = "On tertiary"),
    TERTIARY_CONTAINER(text = "Tertiary container"),
    ON_TERTIARY_CONTAINER(text = "On tertiary container"),
    ERROR(text = "Error"),
    ON_ERROR(text = "On error"),
    ERROR_CONTAINER(text = "Error container"),
    ON_ERROR_CONTAINER(text = "On error container"),
    BACKGROUND(text = "Background"),
    ON_BACKGROUND(text = "On background"),
    SURFACE(text = "Surface"),
    ON_SURFACE(text = "On surface"),
    SURFACE_VARIANT(text = "Surface variant"),
    ON_SURFACE_VARIANT(text = "On surface variant"),
    SURFACE_DIM(text = "Surface dim"),
    SURFACE_BRIGHT(text = "Surface bright"),
    SURFACE_CONTAINER_LOWEST(text = "Surface container lowest"),
    SURFACE_CONTAINER_LOW(text = "Surface container low"),
    SURFACE_CONTAINER(text = "Surface container"),
    SURFACE_CONTAINER_HIGH(text = "Surface container high"),
    SURFACE_CONTAINER_HIGHEST(text = "Surface container highest"),
    INVERSE_SURFACE(text = "Inverse surface"),
    INVERSE_ON_SURFACE(text = "Inverse on surface"),
    INVERSE_PRIMARY(text = "Inverse primary"),
    OUTLINE(text = "Outline"),
    OUTLINE_VARIANT(text = "Outline variant"),
    SCRIM(text = "Scrim");

    /**
     * Resolves the role drawn on top of this one, used to render legible content over it.
     *
     * @return The contrasting role, or `null` when no role contrasts with it.
     */
    fun contentRole(): ColorRole? = when (this) {
        PRIMARY -> ON_PRIMARY
        ON_PRIMARY -> PRIMARY
        PRIMARY_CONTAINER -> ON_PRIMARY_CONTAINER
        ON_PRIMARY_CONTAINER -> PRIMARY_CONTAINER
        SECONDARY -> ON_SECONDARY
        ON_SECONDARY -> SECONDARY
        SECONDARY_CONTAINER -> ON_SECONDARY_CONTAINER
        ON_SECONDARY_CONTAINER -> SECONDARY_CONTAINER
        TERTIARY -> ON_TERTIARY
        ON_TERTIARY -> TERTIARY
        TERTIARY_CONTAINER -> ON_TERTIARY_CONTAINER
        ON_TERTIARY_CONTAINER -> TERTIARY_CONTAINER
        ERROR -> ON_ERROR
        ON_ERROR -> ERROR
        ERROR_CONTAINER -> ON_ERROR_CONTAINER
        ON_ERROR_CONTAINER -> ERROR_CONTAINER
        BACKGROUND -> ON_BACKGROUND
        ON_BACKGROUND -> BACKGROUND
        SURFACE, SURFACE_DIM, SURFACE_BRIGHT, SURFACE_CONTAINER_LOWEST, SURFACE_CONTAINER_LOW, SURFACE_CONTAINER,
        SURFACE_CONTAINER_HIGH, SURFACE_CONTAINER_HIGHEST, OUTLINE_VARIANT -> ON_SURFACE

        ON_SURFACE -> SURFACE
        SURFACE_VARIANT -> ON_SURFACE_VARIANT
        ON_SURFACE_VARIANT -> SURFACE_VARIANT
        INVERSE_SURFACE -> INVERSE_ON_SURFACE
        INVERSE_ON_SURFACE -> INVERSE_SURFACE
        INVERSE_PRIMARY -> ON_PRIMARY
        OUTLINE -> BACKGROUND
        SCRIM -> null
    }

    /**
     * Reads this role's color from a color scheme.
     *
     * @param colorScheme The color scheme to read from.
     * @return The color assigned to this role.
     */
    fun get(colorScheme: ColorScheme): Color = when (this) {
        PRIMARY -> colorScheme.primary
        ON_PRIMARY -> colorScheme.onPrimary
        PRIMARY_CONTAINER -> colorScheme.primaryContainer
        ON_PRIMARY_CONTAINER -> colorScheme.onPrimaryContainer
        SECONDARY -> colorScheme.secondary
        ON_SECONDARY -> colorScheme.onSecondary
        SECONDARY_CONTAINER -> colorScheme.secondaryContainer
        ON_SECONDARY_CONTAINER -> colorScheme.onSecondaryContainer
        TERTIARY -> colorScheme.tertiary
        ON_TERTIARY -> colorScheme.onTertiary
        TERTIARY_CONTAINER -> colorScheme.tertiaryContainer
        ON_TERTIARY_CONTAINER -> colorScheme.onTertiaryContainer
        ERROR -> colorScheme.error
        ON_ERROR -> colorScheme.onError
        ERROR_CONTAINER -> colorScheme.errorContainer
        ON_ERROR_CONTAINER -> colorScheme.onErrorContainer
        BACKGROUND -> colorScheme.background
        ON_BACKGROUND -> colorScheme.onBackground
        SURFACE -> colorScheme.surface
        ON_SURFACE -> colorScheme.onSurface
        SURFACE_VARIANT -> colorScheme.surfaceVariant
        ON_SURFACE_VARIANT -> colorScheme.onSurfaceVariant
        SURFACE_DIM -> colorScheme.surfaceDim
        SURFACE_BRIGHT -> colorScheme.surfaceBright
        SURFACE_CONTAINER_LOWEST -> colorScheme.surfaceContainerLowest
        SURFACE_CONTAINER_LOW -> colorScheme.surfaceContainerLow
        SURFACE_CONTAINER -> colorScheme.surfaceContainer
        SURFACE_CONTAINER_HIGH -> colorScheme.surfaceContainerHigh
        SURFACE_CONTAINER_HIGHEST -> colorScheme.surfaceContainerHighest
        INVERSE_SURFACE -> colorScheme.inverseSurface
        INVERSE_ON_SURFACE -> colorScheme.inverseOnSurface
        INVERSE_PRIMARY -> colorScheme.inversePrimary
        OUTLINE -> colorScheme.outline
        OUTLINE_VARIANT -> colorScheme.outlineVariant
        SCRIM -> colorScheme.scrim
    }

    /**
     * Copies a color scheme with this role's color replaced.
     *
     * @param colorScheme The color scheme to copy.
     * @param color The color to assign to this role.
     * @return The copied color scheme.
     */
    fun set(colorScheme: ColorScheme, color: Color): ColorScheme = when (this) {
        PRIMARY -> colorScheme.copy(primary = color)
        ON_PRIMARY -> colorScheme.copy(onPrimary = color)
        PRIMARY_CONTAINER -> colorScheme.copy(primaryContainer = color)
        ON_PRIMARY_CONTAINER -> colorScheme.copy(onPrimaryContainer = color)
        SECONDARY -> colorScheme.copy(secondary = color)
        ON_SECONDARY -> colorScheme.copy(onSecondary = color)
        SECONDARY_CONTAINER -> colorScheme.copy(secondaryContainer = color)
        ON_SECONDARY_CONTAINER -> colorScheme.copy(onSecondaryContainer = color)
        TERTIARY -> colorScheme.copy(tertiary = color)
        ON_TERTIARY -> colorScheme.copy(onTertiary = color)
        TERTIARY_CONTAINER -> colorScheme.copy(tertiaryContainer = color)
        ON_TERTIARY_CONTAINER -> colorScheme.copy(onTertiaryContainer = color)
        ERROR -> colorScheme.copy(error = color)
        ON_ERROR -> colorScheme.copy(onError = color)
        ERROR_CONTAINER -> colorScheme.copy(errorContainer = color)
        ON_ERROR_CONTAINER -> colorScheme.copy(onErrorContainer = color)
        BACKGROUND -> colorScheme.copy(background = color)
        ON_BACKGROUND -> colorScheme.copy(onBackground = color)
        SURFACE -> colorScheme.copy(surface = color)
        ON_SURFACE -> colorScheme.copy(onSurface = color)
        SURFACE_VARIANT -> colorScheme.copy(surfaceVariant = color)
        ON_SURFACE_VARIANT -> colorScheme.copy(onSurfaceVariant = color)
        SURFACE_DIM -> colorScheme.copy(surfaceDim = color)
        SURFACE_BRIGHT -> colorScheme.copy(surfaceBright = color)
        SURFACE_CONTAINER_LOWEST -> colorScheme.copy(surfaceContainerLowest = color)
        SURFACE_CONTAINER_LOW -> colorScheme.copy(surfaceContainerLow = color)
        SURFACE_CONTAINER -> colorScheme.copy(surfaceContainer = color)
        SURFACE_CONTAINER_HIGH -> colorScheme.copy(surfaceContainerHigh = color)
        SURFACE_CONTAINER_HIGHEST -> colorScheme.copy(surfaceContainerHighest = color)
        INVERSE_SURFACE -> colorScheme.copy(inverseSurface = color)
        INVERSE_ON_SURFACE -> colorScheme.copy(inverseOnSurface = color)
        INVERSE_PRIMARY -> colorScheme.copy(inversePrimary = color)
        OUTLINE -> colorScheme.copy(outline = color)
        OUTLINE_VARIANT -> colorScheme.copy(outlineVariant = color)
        SCRIM -> colorScheme.copy(scrim = color)
    }
}

/**
 * Copies a color scheme with every given role's color replaced.
 *
 * @param colors The colors to assign, keyed by role.
 * @return The copied color scheme, or this one when [colors] is empty.
 */
internal fun ColorScheme.override(colors: Map<ColorRole, Color>): ColorScheme =
    colors.entries.fold(initial = this) { colorScheme, (role, color) -> role.set(colorScheme = colorScheme, color = color) }

/**
 * Converts a color to its hexadecimal representation.
 *
 * @return The color as an uppercase `#RRGGBB` string, or `#AARRGGBB` when it is not fully opaque.
 */
fun Color.toHex(): String {
    val argb = toArgb().toLong() and 0xFFFFFFFF
    return if (argb ushr 24 == 0xFFL) "#${(argb and 0xFFFFFF).toString(radix = 16).padStart(length = 6, padChar = '0')}".uppercase()
    else "#${argb.toString(radix = 16).padStart(length = 8, padChar = '0')}".uppercase()
}

/**
 * Parses a hexadecimal color, with or without its leading `#`.
 *
 * @return The color for a `RRGGBB` or `AARRGGBB` string, or `null` when the string is neither.
 */
fun String.toColorOrNull(): Color? {
    val hex = trim().removePrefix(prefix = "#")
    if (!hex.all { it.isDigit() || it.lowercaseChar() in 'a'..'f' }) return null
    return when (hex.length) {
        6 -> hex.toLongOrNull(radix = 16)?.let { Color(color = 0xFF000000 or it) }
        8 -> hex.toLongOrNull(radix = 16)?.let { Color(color = it) }
        else -> null
    }
}

/**
 * Resolves the current [ColorScheme] based on the system theme, with the [ThemeCache] overrides applied.
 *
 * @return [darkColorScheme] if the system is in dark theme, [lightColorScheme] otherwise, each with its overrides applied.
 */
@Composable
internal fun getColorScheme(): ColorScheme {
    val dark = isSystemInDarkTheme()
    val overrides by ThemeCache.cacheState.collectAsStateWithLifecycle()
    return remember(dark, overrides) {
        (if (dark) darkColorScheme else lightColorScheme).override(colors = overrides.colors(dark = dark))
    }
}
