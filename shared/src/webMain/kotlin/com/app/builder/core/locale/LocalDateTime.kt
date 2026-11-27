package com.app.builder.core.locale

import com.app.builder.core.telemetry.Telemetry

actual fun getLocalDateTime(utc: String, languageIso: String?, parts: DateTimeParts): String = runCatching {
    if (parts.skeleton.isEmpty()) return ""
    val language = languageIso?.takeIf { it != browserLanguageJs().substringBefore(delimiter = "-") }.orEmpty()
    formatDateJs(utc = utc, language = language, year = parts.year, month = parts.month, day = parts.day, hour = parts.hour, minute = parts.minute)
        .also { if (it == "INVALID") throw IllegalArgumentException() }
}.onFailure {
    Telemetry.error(tag = TAG, message = "Unable to get local date time", throwable = it)
}.getOrDefault(defaultValue = utc)

/**
 * Formats the chosen parts of [utc] into a localized string in the browser's time zone using the `Intl` locale of [language], or the browser's when [language] is empty.
 *
 * @param utc The input timestamp string in UTC ISO-8601 format.
 * @param language The language tag whose conventions format the string, or empty for the browser's locale.
 * @param year Whether the year is included.
 * @param month Whether the month is included, as a number.
 * @param day Whether the day of the month is included.
 * @param hour Whether the hour is included.
 * @param minute Whether the minute is included.
 * @return The localized string, or `"INVALID"` if [utc] cannot be parsed.
 */
@JsFun(
    code = """
        (utc, language, year, month, day, hour, minute) => {
            const value = new Date(utc);
            if (isNaN(value.getTime())) return "INVALID";
            const options = {};
            if (year) options.year = 'numeric';
            if (month) options.month = 'numeric';
            if (day) options.day = 'numeric';
            if (hour) options.hour = 'numeric';
            if (minute) options.minute = '2-digit';
            return value.toLocaleString(language || undefined, options);
        }
    """
)
private external fun formatDateJs(utc: String, language: String, year: Boolean, month: Boolean, day: Boolean, hour: Boolean, minute: Boolean): String

/**
 * Returns the language tag of the browser.
 *
 * @return The language tag (e.g., "en-GB").
 */
@JsFun(code = "() => window.navigator.language")
private external fun browserLanguageJs(): String

private const val TAG = "LocalDateTime"
