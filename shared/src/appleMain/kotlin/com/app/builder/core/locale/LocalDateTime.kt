package com.app.builder.core.locale

import kotlin.time.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.offsetAt
import kotlinx.datetime.toNSDate
import platform.Foundation.NSDateFormatter
import platform.Foundation.NSLocale
import platform.Foundation.NSTimeZone
import platform.Foundation.preferredLanguages
import platform.Foundation.timeZoneForSecondsFromGMT
import com.app.builder.core.telemetry.Telemetry

actual fun getLocalDateTime(utc: String, languageIso: String?, parts: DateTimeParts): String = runCatching {
    val skeleton = parts.skeleton
    if (skeleton.isEmpty()) return ""
    val instant = Instant.parse(input = utc)
    val formatter = NSDateFormatter()
    languageIso?.takeIf { it != (NSLocale.preferredLanguages.firstOrNull() as? String)?.substringBefore(delimiter = "-")?.substringBefore(delimiter = "_") }?.let { formatter.locale = NSLocale(localeIdentifier = it) }
    formatter.setLocalizedDateFormatFromTemplate(dateFormatTemplate = skeleton)
    val secondsFromGmt = TimeZone.currentSystemDefault().offsetAt(instant = instant).totalSeconds
    formatter.timeZone = NSTimeZone.timeZoneForSecondsFromGMT(seconds = secondsFromGmt.toLong())
    formatter.stringFromDate(date = instant.toNSDate())
}.onFailure {
    Telemetry.error(tag = TAG, message = "Unable to get local date time", throwable = it)
}.getOrDefault(defaultValue = utc)

private const val TAG = "LocalDateTime"
