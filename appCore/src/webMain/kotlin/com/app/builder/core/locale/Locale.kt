package com.app.builder.core.locale

import kotlinx.browser.window
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import com.app.builder.core.telemetry.Telemetry
import org.w3c.dom.events.Event

actual fun getLanguage(): String = runCatching {
    window.navigator.language.ifBlank { DEFAULT_LANGUAGE }
}.onFailure {
    Telemetry.error(tag = TAG, message = "Unable to get language", throwable = it)
}.getOrDefault(defaultValue = DEFAULT_LANGUAGE)

actual fun getLocalDateTime(utc: String): String = runCatching {
    formatDateJs(utc = utc).also { if (it == "INVALID") throw IllegalArgumentException() }
}.onFailure {
    Telemetry.error(tag = TAG, message = "Unable to get local date time", throwable = it)
}.getOrDefault(defaultValue = utc)

/**
 * Formats [utc] into a short localized date-time string using the browser's `Intl` locale.
 *
 * @param utc The input timestamp string in UTC ISO-8601 format.
 * @return The localized date-time string, or `"INVALID"` if [utc] cannot be parsed.
 */
@JsFun(
    code = """
        (utc) => {
            const date = new Date(utc);
            if (isNaN(date.getTime())) return "INVALID";
            return date.toLocaleString(undefined, {
                dateStyle: 'short',
                timeStyle: 'short'
            });
        }
    """
)
private external fun formatDateJs(utc: String): String

actual fun observeLocale(): Flow<String> = callbackFlow {
    trySend(element = getLanguage())
    val listener: ((Event) -> Unit) = { trySend(element = getLanguage()) }
    window.addEventListener(type = "languagechange", callback = listener)
    awaitClose { window.removeEventListener(type = "languagechange", callback = listener) }
}.distinctUntilChanged()

private const val TAG = "Locale"
