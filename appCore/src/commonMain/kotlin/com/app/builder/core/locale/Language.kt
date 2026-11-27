package com.app.builder.core.locale

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged

/** App default language. */
const val DEFAULT_LANGUAGE = "en"

/**
 * Observes the language the app is shown in, which is the [Language] or, while none is selected, the device language.
 *
 * @param locale Emits the device's ISO 639-1 language code whenever it changes.
 * @return A [Flow] emitting the ISO 639-1 code of the app language.
 */
fun observeLanguage(locale: Flow<String> = observeDeviceLanguage()): Flow<String> =
    combine(
        flow = Language.flow,
        flow2 = locale
    ) { selected, device -> selected ?: device }.distinctUntilChanged()
