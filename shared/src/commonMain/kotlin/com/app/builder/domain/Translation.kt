package com.app.builder.domain

import kotlinx.serialization.Serializable

/**
 * Represents a localized text asset used for internationalization.
 *
 * @property languageIso The ISO language code defining the language context.
 * @property key The unique identifier matching the resource slot across localization maps.
 * @property value The localized text string to be rendered in the user interface.
 */
@Serializable
data class Translation(
    val languageIso: String,
    val key: String,
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