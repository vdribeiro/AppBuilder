package com.app.builder.ui

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ProvidedValue
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.app.builder.core.config.ClientConfigs
import com.app.builder.ui.core.image.RegisterImageLoader

/**
 * The primary styling context for the application.
 *
 * @param compositionValues Additional [ProvidedValue]s injected into the Compose tree.
 * @param content The primary user interface composable hierarchy tree to be wrapped by this theme layer.
 */
@Composable
fun AppTheme(
    compositionValues: List<ProvidedValue<*>> = emptyList(),
    content: @Composable () -> Unit
) = BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
    RegisterImageLoader()
    val splitScreen = maxWidth >= ClientConfigs.configs.splitScreenMaximumWidth.dp
    val translationMap = getTranslationState()
    val typography = getTypography()
    val colorScheme = getColorScheme()
    val providers = remember(
        compositionValues,
        splitScreen,
        translationMap,
        typography,
        colorScheme
    ) {
        buildList {
            addAll(elements = compositionValues)
            add(element = LocalSplitScreen provides splitScreen)
            add(element = LocalTranslationState provides translationMap)
            add(element = LocalTypography provides typography)
            add(element = LocalColorScheme provides colorScheme)
        }.toTypedArray()
    }

    CompositionLocalProvider(values = providers) {
        MaterialTheme(
            colorScheme = colorScheme,
            shapes = LocalShapes.current,
            typography = typography,
            content = content
        )
    }
}

/** CompositionLocal providing if the current display is large enough for a split screen. */
val LocalSplitScreen = compositionLocalOf { false }