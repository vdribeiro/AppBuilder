package com.app.builder.core.locale

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import com.app.builder.core.flow.StateHolder

/** App default language. */
const val DEFAULT_LANGUAGE = "en"

/** Holds the ISO 639-1 code of the language selected for the app, or `null` while the app follows the device language. */
object SelectedLanguage: StateHolder<String?>(initialValue = null)

/**
 * Observes the language the app is shown in, which is the [SelectedLanguage] or, while none is selected, the device language.
 *
 * @param locale Emits the device's ISO 639-1 language code whenever it changes.
 * @return A [Flow] emitting the ISO 639-1 code of the app language.
 */
fun observeLanguage(locale: Flow<String> = observeLocale()): Flow<String> =
    combine(
        flow = SelectedLanguage.flow,
        flow2 = locale
    ) { selected, device -> selected ?: device }.distinctUntilChanged()
