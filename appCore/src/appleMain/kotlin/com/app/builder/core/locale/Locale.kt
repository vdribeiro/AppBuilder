package com.app.builder.core.locale

import kotlin.time.Instant
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.datetime.TimeZone
import kotlinx.datetime.offsetAt
import kotlinx.datetime.toNSDate
import platform.Foundation.NSCurrentLocaleDidChangeNotification
import platform.Foundation.NSDateFormatter
import platform.Foundation.NSDateFormatterNoStyle
import platform.Foundation.NSDateFormatterShortStyle
import platform.Foundation.NSDateFormatterStyle
import platform.Foundation.NSLocale
import platform.Foundation.NSNotificationCenter
import platform.Foundation.NSTimeZone
import platform.Foundation.preferredLanguages
import platform.Foundation.timeZoneForSecondsFromGMT
import com.app.builder.core.platform.observe
import com.app.builder.core.telemetry.Telemetry

actual fun getDeviceLanguage(): String = runCatching {
    (NSLocale.preferredLanguages.first() as String).substringBefore(delimiter = "-").substringBefore(delimiter = "_")
}.onFailure {
    Telemetry.error(tag = TAG, message = "Unable to get language", throwable = it)
}.getOrDefault(defaultValue = DEFAULT_LANGUAGE)

actual fun observeDeviceLanguage(): Flow<String> = callbackFlow {
    trySend(element = getDeviceLanguage())
    val observer = NSNotificationCenter.defaultCenter.observe(
        name = NSCurrentLocaleDidChangeNotification
    ) { trySend(element = getDeviceLanguage()) }
    awaitClose { NSNotificationCenter.defaultCenter.removeObserver(observer) }
}.distinctUntilChanged()

actual fun getLocalDateTime(utc: String, languageIso: String?): String = runCatching {
    formatLocal(utc = utc, languageIso = languageIso, dateStyle = NSDateFormatterShortStyle, timeStyle = NSDateFormatterShortStyle)
}.onFailure {
    Telemetry.error(tag = TAG, message = "Unable to get local date time", throwable = it)
}.getOrDefault(defaultValue = utc)

actual fun getLocalDate(utc: String, languageIso: String?): String = runCatching {
    formatLocal(utc = utc, languageIso = languageIso, dateStyle = NSDateFormatterShortStyle, timeStyle = NSDateFormatterNoStyle)
}.onFailure {
    Telemetry.error(tag = TAG, message = "Unable to get local date", throwable = it)
}.getOrDefault(defaultValue = utc)

actual fun getLocalTime(utc: String, languageIso: String?): String = runCatching {
    formatLocal(utc = utc, languageIso = languageIso, dateStyle = NSDateFormatterNoStyle, timeStyle = NSDateFormatterShortStyle)
}.onFailure {
    Telemetry.error(tag = TAG, message = "Unable to get local time", throwable = it)
}.getOrDefault(defaultValue = utc)

/**
 * Formats [utc] in the device's time zone with the given styles, in the conventions of [languageIso].
 *
 * @param utc The input timestamp string in UTC ISO-8601 format.
 * @param languageIso The ISO 639-1 code of the language whose conventions format the string, or `null` for the device locale.
 * @param dateStyle The style of the date.
 * @param timeStyle The style of the time of day.
 * @return The localized string.
 */
private fun formatLocal(utc: String, languageIso: String?, dateStyle: NSDateFormatterStyle, timeStyle: NSDateFormatterStyle): String {
    val instant = Instant.parse(input = utc)
    val formatter = NSDateFormatter()
    formatter.dateStyle = dateStyle
    formatter.timeStyle = timeStyle
    languageIso?.takeIf { it != getDeviceLanguage() }?.let { formatter.locale = NSLocale(localeIdentifier = it) }
    val secondsFromGmt = TimeZone.currentSystemDefault().offsetAt(instant = instant).totalSeconds
    formatter.timeZone = NSTimeZone.timeZoneForSecondsFromGMT(seconds = secondsFromGmt.toLong())
    return formatter.stringFromDate(date = instant.toNSDate())
}

private const val TAG = "Locale"