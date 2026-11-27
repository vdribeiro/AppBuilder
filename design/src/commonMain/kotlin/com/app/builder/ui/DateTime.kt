package com.app.builder.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import com.app.builder.core.locale.DateTimeParts
import com.app.builder.core.locale.Language
import com.app.builder.core.locale.getLocalDateTime

/**
 * Formats and remembers the chosen parts of a UTC ISO-8601 string as a localized string in the device's time zone and the conventions of the [Language], formatted again when it changes.
 *
 * @param utc The input timestamp string in UTC ISO-8601 format.
 * @param parts The parts of [utc] that are included.
 * @return The localized string, or an empty string when no part is included, or [utc] as given if formatting fails.
 */
@Composable
fun getDateTime(
    utc: String,
    parts: DateTimeParts = DateTimeParts(),
): String {
    val language by Language.flow.collectAsState()
    return remember(key1 = utc, key2 = language, key3 = parts) {
        getLocalDateTime(utc = utc, languageIso = language, parts = parts)
    }
}
