package com.app.builder.data.translation

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/** In-memory cache for localized strings. */
object TranslationCache {

    /** Backing mutable state flow holding the current translation map. */
    private val _cacheState = MutableStateFlow(value = emptyMap<String, String>())
    /** Read-only state flow emitting the current map of localized keys and values. */
    val cacheState: StateFlow<Map<String, String>> = _cacheState.asStateFlow()

    /**
     * Checks if the cache is empty.
     *
     * @return `true` if the cache is empty, `false` otherwise.
     */
    fun isEmpty(): Boolean = cacheState.value.isEmpty()

    /**
     * Replaces the current cache contents with the provided translations.
     *
     * @param translations The list of new translations to apply.
     */
    fun set(translations: Map<String, String>) =
        _cacheState.update { translations }

    /**
     * Resolves a localized string matching the specified key.
     *
     * @param key The key mapping to a localized string.
     * @param default The value to return if no matching [key] is found.
     * @param args Arguments used to populate positional placeholders (e.g., %1$s).
     * @return The formatted localized string, or [default] if no match is found.
     */
    fun get(key: String, default: String = key, vararg args: String): String =
        _cacheState.value.getTranslation(key = key, default = default, args = args)

    /**
     * Retrieves the value for a key from the map and replaces positional placeholders with the provided arguments.
     *
     * @receiver The map containing key-to-translation mappings.
     * @param key The key mapping to a localized string.
     * @param default The value to return if no matching [key] is found.
     * @param args Arguments used to populate positional placeholders (e.g., %1$s).
     * @return The formatted localized string, or [default] if the key is not found in the map.
     */
    private fun Map<String, String>.getTranslation(key: String, default: String, vararg args: String): String {
        val rawValue = this[key] ?: default
        return if (args.isEmpty()) rawValue else args.foldIndexed(initial = rawValue) { index, translation, arg ->
            translation.replace(oldValue = $$"%$${index + 1}$s", newValue = arg)
        }
    }
}