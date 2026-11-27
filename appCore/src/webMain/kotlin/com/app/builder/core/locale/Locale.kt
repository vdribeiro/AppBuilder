package com.app.builder.core.locale

import kotlinx.browser.window
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import org.w3c.dom.events.Event
import com.app.builder.core.telemetry.Telemetry

actual fun getDeviceLanguage(): String = runCatching {
    window.navigator.language.substringBefore(delimiter = "-").ifBlank { DEFAULT_LANGUAGE }
}.onFailure {
    Telemetry.error(tag = TAG, message = "Unable to get language", throwable = it)
}.getOrDefault(defaultValue = DEFAULT_LANGUAGE)

actual fun observeDeviceLanguage(): Flow<String> = callbackFlow {
    trySend(element = getDeviceLanguage())
    val listener: ((Event) -> Unit) = { trySend(element = getDeviceLanguage()) }
    window.addEventListener(type = "languagechange", callback = listener)
    awaitClose { window.removeEventListener(type = "languagechange", callback = listener) }
}.distinctUntilChanged()

actual fun getLocalDateTime(utc: String, languageIso: String?): String = runCatching {
    formatLocal(utc = utc, languageIso = languageIso, date = true, time = true)
}.onFailure {
    Telemetry.error(tag = TAG, message = "Unable to get local date time", throwable = it)
}.getOrDefault(defaultValue = utc)

actual fun getLocalDate(utc: String, languageIso: String?): String = runCatching {
    formatLocal(utc = utc, languageIso = languageIso, date = true, time = false)
}.onFailure {
    Telemetry.error(tag = TAG, message = "Unable to get local date", throwable = it)
}.getOrDefault(defaultValue = utc)

actual fun getLocalTime(utc: String, languageIso: String?): String = runCatching {
    formatLocal(utc = utc, languageIso = languageIso, date = false, time = true)
}.onFailure {
    Telemetry.error(tag = TAG, message = "Unable to get local time", throwable = it)
}.getOrDefault(defaultValue = utc)

/**
 * Formats the date, the time of day, or both of [utc] in the browser's time zone, in the conventions of [languageIso].
 *
 * @param utc The input timestamp string in UTC ISO-8601 format.
 * @param languageIso The ISO 639-1 code of the language whose conventions format the string, or `null` for the device locale.
 * @param date Whether the date is formatted.
 * @param time Whether the time of day is formatted.
 * @return The localized string.
 * @throws IllegalArgumentException If [utc] cannot be parsed.
 */
private fun formatLocal(utc: String, languageIso: String?, date: Boolean, time: Boolean): String =
    formatDateJs(utc = utc, language = languageIso?.takeIf { it != getDeviceLanguage() }.orEmpty(), date = date, time = time).also { if (it == "INVALID") throw IllegalArgumentException() }

/**
 * Formats the date, the time of day, or both of [utc] into a short localized string using the `Intl` locale of [language], or the browser's when [language] is empty.
 *
 * @param utc The input timestamp string in UTC ISO-8601 format.
 * @param language The language tag whose conventions format the string, or empty for the browser's locale.
 * @param date Whether the date is formatted.
 * @param time Whether the time of day is formatted.
 * @return The localized string, or `"INVALID"` if [utc] cannot be parsed.
 */
@JsFun(
    code = """
        (utc, language, date, time) => {
            const value = new Date(utc);
            if (isNaN(value.getTime())) return "INVALID";
            const options = {};
            if (date) options.dateStyle = 'short';
            if (time) options.timeStyle = 'short';
            return value.toLocaleString(language || undefined, options);
        }
    """
)
private external fun formatDateJs(utc: String, language: String, date: Boolean, time: Boolean): String

private const val TAG = "Locale"
