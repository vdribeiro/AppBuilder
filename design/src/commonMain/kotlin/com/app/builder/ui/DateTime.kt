package com.app.builder.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf

/** The parts of a date and time that are shown and picked. */
enum class DateTimeMode {
    /** The date and the time of day. */
    DATE_TIME,
    /** The date. */
    DATE,
    /** The time of day. */
    TIME
}

/**
 * CompositionLocal providing the function that formats the parts of a UTC ISO-8601 string named by a [DateTimeMode] into a localized string.
 * Returns the string as given unless a formatter is provided.
 */
val LocalDateTimeFormatter: ProvidableCompositionLocal<(String, DateTimeMode) -> String> = staticCompositionLocalOf { { value, _ -> value } }

/**
 * Formats and remembers the parts of a UTC ISO-8601 string named by [mode] as a localized string with the [LocalDateTimeFormatter].
 *
 * @param utc The input timestamp string in UTC ISO-8601 format.
 * @param mode The parts of [utc] to show.
 * @return The localized string.
 */
@Composable
fun getDateTime(utc: String, mode: DateTimeMode = DateTimeMode.DATE_TIME): String {
    val formatter = LocalDateTimeFormatter.current
    return remember(utc, mode, formatter) { formatter(utc, mode) }
}
