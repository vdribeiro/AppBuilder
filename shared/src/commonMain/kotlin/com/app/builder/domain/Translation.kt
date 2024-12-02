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
)