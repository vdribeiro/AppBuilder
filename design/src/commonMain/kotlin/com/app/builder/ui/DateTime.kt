package com.app.builder.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf

/**
 * CompositionLocal providing the function that formats a UTC ISO-8601 string into a localized date-time string.
 * Returns the string as given unless a formatter is provided.
 */
val LocalDateTimeFormatter: ProvidableCompositionLocal<(String) -> String> = staticCompositionLocalOf { { it } }

/**
 * Formats and remembers a UTC ISO-8601 string as a localized date-time string with the [LocalDateTimeFormatter].
 *
 * @param utc The input timestamp string in UTC ISO-8601 format.
 * @return The localized date-time string.
 */
@Composable
fun getDateTime(utc: String): String {
    val formatter = LocalDateTimeFormatter.current
    return remember(key1 = utc, key2 = formatter) { formatter(utc) }
}
