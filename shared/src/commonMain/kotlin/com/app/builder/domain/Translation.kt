package com.app.builder.domain

import kotlin.time.Instant
import kotlinx.serialization.Serializable
import com.app.builder.core.locale.UNIX_EPOCH
import com.app.builder.data.serializer.decode

/**
 * Represents a localized text asset used for internationalization.
 *
 * @property languageIso The ISO language code defining the language context.
 * @property key The unique identifier matching the resource slot across localization maps.
 * @property modifiedAt The timestamp indicating when this translation was last updated.
 * @property value The localized text string to be rendered in the user interface.
 */
@Serializable
data class Translation(
    val languageIso: String,
    val key: String,
    val modifiedAt: Instant = Instant.UNIX_EPOCH,
    val value: String
) {
    /**
     * Enumerates the [Translation] fields that map to a localized display label.
     *
     * @property translationKey The [Translation.key] used to look up the localized label for this field.
     */
    @Serializable
    enum class Property(val translationKey: String) {
        KEY(translationKey = "translation_key"),
        VALUE(translationKey = "translation_value")
    }
}

/**
 * Decodes the content of a translations file into its [Translation] entries.
 * The file groups its keys by subject, and each key maps a language ISO code to its localized value.
 *
 * @param value The raw JSON content of the translations file.
 * @return The [Translation] entries in file order, or null if decoding fails.
 */
fun decodeTranslations(value: String): List<Translation>? =
    decode<Map<String, Map<String, Map<String, String>>>>(value = value)?.values?.flatMap { keys ->
        keys.flatMap { (key, values) ->
            values.map { (languageIso, value) -> Translation(languageIso = languageIso, key = key, value = value) }
        }
    }
