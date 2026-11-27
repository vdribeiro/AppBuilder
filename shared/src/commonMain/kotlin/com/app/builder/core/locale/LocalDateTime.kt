package com.app.builder.core.locale

/**
 * The parts of a date and time that a formatted string includes.
 *
 * @property year Whether the year is included.
 * @property month Whether the month is included, as a number.
 * @property day Whether the day of the month is included.
 * @property hour Whether the hour is included.
 * @property minute Whether the minute is included.
 */
data class DateTimeParts(
    val year: Boolean = true,
    val month: Boolean = true,
    val day: Boolean = true,
    val hour: Boolean = true,
    val minute: Boolean = true,
) {
    /** The Unicode date-time skeleton naming the included parts (e.g., "yMdjm"), with the hour in the 12-hour or 24-hour convention of the locale it is resolved in. Empty when no part is included. */
    internal val skeleton: String
        get() = buildString {
            if (year) append("y")
            if (month) append("M")
            if (day) append("d")
            if (hour) append("j")
            if (minute) append("m")
        }
}

/**
 * Formats the chosen parts of a UTC ISO-8601 string into a localized string in the device's time zone.
 * The parts are ordered and separated by the conventions of the language, and the hour follows its 12-hour or 24-hour convention.
 *
 * @param utc The input timestamp string in UTC ISO-8601 format.
 * @param languageIso The ISO 639-1 code of the language whose conventions format the string, or `null` for the device locale.
 * @param parts The parts of [utc] that are included.
 * @return The localized string, an empty string when no part is included, or the original [utc] string if formatting fails.
 */
expect fun getLocalDateTime(
    utc: String = now().toString(),
    languageIso: String? = Language.value,
    parts: DateTimeParts = DateTimeParts(),
): String
