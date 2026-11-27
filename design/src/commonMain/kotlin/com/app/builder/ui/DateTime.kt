package com.app.builder.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.Stable
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
 * Converts date-times between UTC and a local time zone.
 *
 * @property toLocal Converts a UTC ISO-8601 string into the local date-time, as an ISO-8601 string without seconds or offset, such as `2026-11-27T01:15`.
 * @property toUtc Converts a local date-time, as an ISO-8601 string without seconds or offset, into a UTC ISO-8601 string.
 */
@Stable
data class DateTimeConverter(
    val toLocal: (String) -> String = { it.take(n = 16) },
    val toUtc: (String) -> String = { "$it:00Z" },
)

/**
 * CompositionLocal providing the [DateTimeConverter].
 * Takes UTC as the local time zone unless a converter is provided.
 */
val LocalDateTimeConverter: ProvidableCompositionLocal<DateTimeConverter> = staticCompositionLocalOf { DateTimeConverter() }

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
