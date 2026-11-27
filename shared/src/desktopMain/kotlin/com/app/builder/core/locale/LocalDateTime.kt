package com.app.builder.core.locale

import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.time.Instant
import kotlin.time.toJavaInstant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toJavaZoneId
import com.app.builder.core.telemetry.Telemetry

actual fun getLocalDateTime(utc: String, languageIso: String?, parts: DateTimeParts): String = runCatching {
    val skeleton = parts.skeleton
    if (skeleton.isEmpty()) return ""
    DateTimeFormatter.ofLocalizedPattern(skeleton)
        .withLocale(languageIso?.takeIf { it != Locale.getDefault().language }?.let { Locale.forLanguageTag(it) } ?: Locale.getDefault(Locale.Category.FORMAT))
        .withZone(TimeZone.currentSystemDefault().toJavaZoneId())
        .format(Instant.parse(input = utc).toJavaInstant())
}.onFailure {
    Telemetry.error(tag = TAG, message = "Unable to get local date time", throwable = it)
}.getOrDefault(defaultValue = utc)

private const val TAG = "LocalDateTime"
