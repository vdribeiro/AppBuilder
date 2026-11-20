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
    formatDateJs(utc = utc, language = languageIso?.takeIf { it != getDeviceLanguage() }.orEmpty()).also { if (it == "INVALID") throw IllegalArgumentException() }
}.onFailure {
    Telemetry.error(tag = TAG, message = "Unable to get local date time", throwable = it)
}.getOrDefault(defaultValue = utc)

/**
 * Formats [utc] into a short localized date-time string using the `Intl` locale of [language], or the browser's when [language] is empty.
 *
 * @param utc The input timestamp string in UTC ISO-8601 format.
 * @param language The language tag whose conventions format the string, or empty for the browser's locale.
 * @return The localized date-time string, or `"INVALID"` if [utc] cannot be parsed.
 */
@JsFun(
    code = """
        (utc, language) => {
            const date = new Date(utc);
            if (isNaN(date.getTime())) return "INVALID";
            return date.toLocaleString(language || undefined, {
                dateStyle: 'short',
                timeStyle: 'short'
            });
        }
    """
)
private external fun formatDateJs(utc: String, language: String): String

private const val TAG = "Locale"
