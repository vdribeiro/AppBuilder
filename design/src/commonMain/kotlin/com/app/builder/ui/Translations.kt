package com.app.builder.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.app.builder.data.translation.TranslationCache

/**
 * CompositionLocal providing the current map of localized keys and values.
 * Uses [staticCompositionLocalOf] because translations are updated as a complete batch rather than frequent individual node changes.
 */
val LocalTranslationState: ProvidableCompositionLocal<Map<String, String>> = staticCompositionLocalOf { TranslationCache.cacheState.value }

/**
 * Collects the translation cache state as a Compose state following the host lifecycle.
 *
 * @return A map of localized keys and values.
 */
@Composable
fun getTranslationState(): Map<String, String> {
    val state by TranslationCache.cacheState.collectAsStateWithLifecycle()
    return state
}

/**
 * Resolves and remembers a localized string matching the specified key.
 *
 * @param key The key mapping to a localized string.
 * @param default The value to return if no matching [key] is found.
 * @param args Arguments used to populate positional placeholders.
 * @return The formatted localized string.
 */
@Composable
fun getTranslation(key: String, default: String = key, vararg args: String): String {
    val cacheState = LocalTranslationState.current
    return remember(cacheState, key, *args) { TranslationCache.get(key = key, args = args) }
}

/**
 * Seeds mock translation data into the translation cache for UI previews.
 *
 * @param translations The preview translations to inject.
 */
@Composable
fun InjectTranslations(translations: Map<String, String>) {
    TranslationCache.set(translations = translations)
}
