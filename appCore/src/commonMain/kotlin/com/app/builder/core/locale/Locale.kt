package com.app.builder.core.locale

import kotlinx.coroutines.flow.Flow

/**
 * Returns the ISO 639-1 two-letter code of the device language.
 *
 * @return The device language code (e.g., "en", "pt").
 */
expect fun getDeviceLanguage(): String

/**
 * Observes the device language.
 *
 * @return A [Flow] emitting the ISO 639-1 code of the device language, and the new one whenever it changes.
 */
expect fun observeDeviceLanguage(): Flow<String>

/**
 * Formats a UTC ISO-8601 string into a localized date-time string.
 *
 * @param utc The input timestamp string in UTC ISO-8601 format.
 * @param languageIso The ISO 639-1 code of the language whose conventions format the string, or `null` for the device locale.
 * @return The localized date-time string, or the original [utc] string if formatting fails.
 */
expect fun getLocalDateTime(utc: String = now().toString(), languageIso: String? = SelectedLanguage.value): String
