package com.app.builder.data.translation

import com.app.builder.core.flow.StateHolder

/** In-memory cache for localized strings, holding the map of localized keys and values. */
object TranslationCache: StateHolder<Map<String, String>>(default = emptyMap()) {

    /**
     * Resolves a localized string matching the specified key.
     *
     * @param key The key mapping to a localized string.
     * @param default The value to return if no matching [key] is found.
     * @param args Arguments used to populate positional placeholders (e.g., %1$s).
     * @return The formatted localized string, or [default] if no match is found.
     */
    fun get(key: String, default: String = key, vararg args: String): String =
        value.getTranslation(key = key, default = default, args = args)

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