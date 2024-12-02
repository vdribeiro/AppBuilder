package com.app.builder.core.locale

import kotlinx.coroutines.flow.Flow

/**
 * Returns the current ISO 639-1 two-letter language code.
 *
 * @return The active language code (e.g., "en", "pt").
 */
expect fun getLanguage(): String

/**
 * Formats a UTC ISO-8601 string into a localized date-time string.
 *
 * @param utc The input timestamp string in UTC ISO-8601 format. Defaults to the current timestamp.
 * @return The localized date-time string, or the original [utc] string if formatting fails.
 */
expect fun getLocalDateTime(utc: String): String

/**
 * Observes system-level locale changes.
 *
 * @return A [Flow] emitting the updated ISO 639-1 language code when changes occur.
 */
expect fun observeLocale(): Flow<String>
