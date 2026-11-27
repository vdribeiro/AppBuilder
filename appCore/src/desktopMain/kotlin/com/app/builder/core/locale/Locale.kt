package com.app.builder.core.locale

import java.util.Locale
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flow
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

private const val TAG = "Locale"
