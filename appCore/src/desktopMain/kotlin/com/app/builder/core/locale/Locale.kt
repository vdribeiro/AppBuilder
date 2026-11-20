package com.app.builder.core.locale

import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale
import kotlin.time.Instant
import kotlin.time.toJavaInstant
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flow
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toJavaZoneId
import com.app.builder.core.platform.loop
import com.app.builder.core.telemetry.Telemetry

actual fun getDeviceLanguage(): String = runCatching {
    Locale.getDefault().language.ifBlank { DEFAULT_LANGUAGE }
}.onFailure {
    Telemetry.error(tag = TAG, message = "Unable to get language", throwable = it)
}.getOrDefault(defaultValue = DEFAULT_LANGUAGE)

actual fun observeDeviceLanguage(): Flow<String> = flow {
    loop { emit(value = getDeviceLanguage()) }
}.distinctUntilChanged()

actual fun getLocalDateTime(utc: String, languageIso: String?): String = runCatching {
    DateTimeFormatter
        .ofLocalizedDateTime(FormatStyle.SHORT)
        .withLocale(languageIso?.takeIf { it != getDeviceLanguage() }?.let { Locale.forLanguageTag(it) } ?: Locale.getDefault(Locale.Category.FORMAT))
        .withZone(TimeZone.currentSystemDefault().toJavaZoneId())
        .format(Instant.parse(input = utc).toJavaInstant())
}.onFailure {
    Telemetry.error(tag = TAG, message = "Unable to get local date time", throwable = it)
}.getOrDefault(defaultValue = utc)

private const val TAG = "Locale"
