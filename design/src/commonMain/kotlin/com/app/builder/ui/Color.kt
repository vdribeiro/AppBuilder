package com.app.builder.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/** The primary color for the light color scheme. */
val primaryLight = Color(0xFF36618E)
/** The on primary color for the light color scheme. */
val onPrimaryLight = Color(0xFFFFFFFF)
/** The primary container color for the light color scheme. */
val primaryContainerLight = Color(0xFFD1E4FF)
/** The on primary container color for the light color scheme. */
val onPrimaryContainerLight = Color(0xFF1A4975)
/** The secondary color for the light color scheme. */
val secondaryLight = Color(0xFF535F70)
/** The on secondary color for the light color scheme. */
val onSecondaryLight = Color(0xFFFFFFFF)
/** The secondary container color for the light color scheme. */
val secondaryContainerLight = Color(0xFFD7E3F8)
/** The on secondary container color for the light color scheme. */
val onSecondaryContainerLight = Color(0xFF3B4858)
/** The tertiary color for the light color scheme. */
val tertiaryLight = Color(0xFF6B5778)
/** The on tertiary color for the light color scheme. */
val onTertiaryLight = Color(0xFFFFFFFF)
/** The tertiary container color for the light color scheme. */
val tertiaryContainerLight = Color(0xFFF3DAFF)
/** The on tertiary container color for the light color scheme. */
val onTertiaryContainerLight = Color(0xFF523F5F)
/** The error color for the light color scheme. */
val errorLight = Color(0xFFBA1A1A)
/** The on error color for the light color scheme. */
val onErrorLight = Color(0xFFFFFFFF)
/** The error container color for the light color scheme. */
val errorContainerLight = Color(0xFFFFDAD6)
/** The on error container color for the light color scheme. */
val onErrorContainerLight = Color(0xFF93000A)
/** The background color for the light color scheme. */
val backgroundLight = Color(0xFFF8F9FF)
/** The on background color for the light color scheme. */
val onBackgroundLight = Color(0xFF191C20)
/** The surface color for the light color scheme. */
val surfaceLight = Color(0xFFF8F9FF)
/** The on surface color for the light color scheme. */
val onSurfaceLight = Color(0xFF191C20)
/** The surface variant color for the light color scheme. */
val surfaceVariantLight = Color(0xFFDFE2EB)
/** The on surface variant color for the light color scheme. */
val onSurfaceVariantLight = Color(0xFF43474E)
/** The outline color for the light color scheme. */
val outlineLight = Color(0xFF73777F)
/** The outline variant color for the light color scheme. */
val outlineVariantLight = Color(0xFFC3C6CF)
/** The scrim color for the light color scheme. */
val scrimLight = Color(0xFF000000)
/** The inverse surface color for the light color scheme. */
val inverseSurfaceLight = Color(0xFF2E3135)
/** The inverse on surface color for the light color scheme. */
val inverseOnSurfaceLight = Color(0xFFEFF0F7)
/** The inverse primary color for the light color scheme. */
val inversePrimaryLight = Color(0xFFA0CAFD)
/** The surface dim color for the light color scheme. */
val surfaceDimLight = Color(0xFFD8DAE0)
/** The surface bright color for the light color scheme. */
val surfaceBrightLight = Color(0xFFF8F9FF)
/** The surface container lowest color for the light color scheme. */
val surfaceContainerLowestLight = Color(0xFFFFFFFF)
/** The surface container low color for the light color scheme. */
val surfaceContainerLowLight = Color(0xFFF2F3FA)
/** The surface container color for the light color scheme. */
val surfaceContainerLight = Color(0xFFECEEF4)
/** The surface container high color for the light color scheme. */
val surfaceContainerHighLight = Color(0xFFE6E8EE)
/** The surface container highest color for the light color scheme. */
val surfaceContainerHighestLight = Color(0xFFE1E2E8)

/** The primary color for the dark color scheme. */
val primaryDark = Color(0xFFA0CAFD)
/** The on primary color for the dark color scheme. */
val onPrimaryDark = Color(0xFF003258)
/** The primary container color for the dark color scheme. */
val primaryContainerDark = Color(0xFF1A4975)
/** The on primary container color for the dark color scheme. */
val onPrimaryContainerDark = Color(0xFFD1E4FF)
/** The secondary color for the dark color scheme. */
val secondaryDark = Color(0xFFBBC7DB)
/** The on secondary color for the dark color scheme. */
val onSecondaryDark = Color(0xFF253140)
/** The secondary container color for the dark color scheme. */
val secondaryContainerDark = Color(0xFF3B4858)
/** The on secondary container color for the dark color scheme. */
val onSecondaryContainerDark = Color(0xFFD7E3F8)
/** The tertiary color for the dark color scheme. */
val tertiaryDark = Color(0xFFD7BEE4)
/** The on tertiary color for the dark color scheme. */
val onTertiaryDark = Color(0xFF3B2948)
/** The tertiary container color for the dark color scheme. */
val tertiaryContainerDark = Color(0xFF523F5F)
/** The on tertiary container color for the dark color scheme. */
val onTertiaryContainerDark = Color(0xFFF3DAFF)
/** The error color for the dark color scheme. */
val errorDark = Color(0xFFFFB4AB)
/** The on error color for the dark color scheme. */
val onErrorDark = Color(0xFF690005)
/** The error container color for the dark color scheme. */
val errorContainerDark = Color(0xFF93000A)
/** The on error container color for the dark color scheme. */
val onErrorContainerDark = Color(0xFFFFDAD6)
/** The background color for the dark color scheme. */
val backgroundDark = Color(0xFF111418)
/** The on background color for the dark color scheme. */
val onBackgroundDark = Color(0xFFE1E2E8)
/** The surface color for the dark color scheme. */
val surfaceDark = Color(0xFF111418)
/** The on surface color for the dark color scheme. */
val onSurfaceDark = Color(0xFFE1E2E8)
/** The surface variant color for the dark color scheme. */
val surfaceVariantDark = Color(0xFF43474E)
/** The on surface variant color for the dark color scheme. */
val onSurfaceVariantDark = Color(0xFFC3C6CF)
/** The outline color for the dark color scheme. */
val outlineDark = Color(0xFF8D9199)
/** The outline variant color for the dark color scheme. */
val outlineVariantDark = Color(0xFF43474E)
/** The scrim color for the dark color scheme. */
val scrimDark = Color(0xFF000000)
/** The inverse surface color for the dark color scheme. */
val inverseSurfaceDark = Color(0xFFE1E2E8)
/** The inverse on surface color for the dark color scheme. */
val inverseOnSurfaceDark = Color(0xFF2E3135)
/** The inverse primary color for the dark color scheme. */
val inversePrimaryDark = Color(0xFF36618E)
/** The surface dim color for the dark color scheme. */
val surfaceDimDark = Color(0xFF111418)
/** The surface bright color for the dark color scheme. */
val surfaceBrightDark = Color(0xFF36393E)
/** The surface container lowest color for the dark color scheme. */
val surfaceContainerLowestDark = Color(0xFF0B0E13)
/** The surface container low color for the dark color scheme. */
val surfaceContainerLowDark = Color(0xFF191C20)
/** The surface container color for the dark color scheme. */
val surfaceContainerDark = Color(0xFF1D2024)
/** The surface container high color for the dark color scheme. */
val surfaceContainerHighDark = Color(0xFF272A2F)
/** The surface container highest color for the dark color scheme. */
val surfaceContainerHighestDark = Color(0xFF32353A)

/** The [ColorScheme] used when the app is displayed with a light theme. */
val lightColorScheme = lightColorScheme(
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
val darkColorScheme = darkColorScheme(
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
 * Resolves the current [ColorScheme] based on the system theme.
 *
 * @return [darkColorScheme] if the system is in dark theme, [lightColorScheme] otherwise.
 */
@Composable
fun getColorScheme(): ColorScheme = if (isSystemInDarkTheme()) darkColorScheme else lightColorScheme
