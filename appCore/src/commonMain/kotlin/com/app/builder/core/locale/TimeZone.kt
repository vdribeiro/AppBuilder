package com.app.builder.core.locale

import kotlin.time.Instant
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import com.app.builder.core.telemetry.Telemetry

/**
 * Converts a UTC ISO-8601 string into the date and time of day of the device's time zone.
 *
 * @param utc The timestamp string in UTC ISO-8601 format.
 * @return The local date-time as an ISO-8601 string without seconds or offset, such as `2026-11-27T01:15`, or [utc] as given when it is invalid.
 */
fun utcToLocal(utc: String): String = utcToLocal(utc = utc, timeZone = TimeZone.currentSystemDefault())

/**
 * Converts a date and time of day of the device's time zone into a UTC ISO-8601 string.
 *
 * @param local The local date-time as an ISO-8601 string without offset, such as `2026-11-27T01:15`.
 * @return The timestamp string in UTC ISO-8601 format, or [local] as given when it is invalid.
 */
fun localToUtc(local: String): String = localToUtc(local = local, timeZone = TimeZone.currentSystemDefault())

/**
 * Converts a UTC ISO-8601 string into the date and time of day of [timeZone].
 *
 * @param utc The timestamp string in UTC ISO-8601 format.
 * @param timeZone The time zone of the local date-time.
 * @return The local date-time as an ISO-8601 string without seconds or offset, or [utc] as given when it is invalid.
 */
internal fun utcToLocal(utc: String, timeZone: TimeZone): String = runCatching {
    val local = Instant.parse(input = utc).toLocalDateTime(timeZone = timeZone)
    LocalDateTime(date = local.date, time = LocalTime(hour = local.hour, minute = local.minute)).toString()
}.onFailure {
    Telemetry.error(tag = TAG, message = "Unable to convert utc to local", throwable = it)
}.getOrDefault(defaultValue = utc)

/**
 * Converts a date and time of day of [timeZone] into a UTC ISO-8601 string.
 *
 * @param local The local date-time as an ISO-8601 string without offset.
 * @param timeZone The time zone of the local date-time.
 * @return The timestamp string in UTC ISO-8601 format, or [local] as given when it is invalid.
 */
internal fun localToUtc(local: String, timeZone: TimeZone): String = runCatching {
    LocalDateTime.parse(input = local).toInstant(timeZone = timeZone).toString()
}.onFailure {
    Telemetry.error(tag = TAG, message = "Unable to convert local to utc", throwable = it)
}.getOrDefault(defaultValue = local)

private const val TAG = "TimeZone"
