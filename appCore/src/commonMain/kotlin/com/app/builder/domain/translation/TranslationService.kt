package com.app.builder.domain.translation

import com.app.builder.core.locale.DEFAULT_LANGUAGE
import com.app.builder.core.locale.observeLocale
import com.app.builder.data.translation.TranslationCache
import com.app.builder.domain.Translation

/** Service to monitor locale and translations. */
interface TranslationService {

    /** Stops observing translations, suspending until the active flow collection has fully canceled. */
    suspend fun stop()

    /**
     * Constructs and executes the reactive pipeline for translation data.
     * If the observer is already running, this is a no-op.
     *
     * ### Resolution Flow:
     * 1. Listens to [observeLocale].
     * 2. Queries the local data for the current language ISO.
     * 3. If the local data returns empty, falls back to the [DEFAULT_LANGUAGE].
     * 4. Finally, pushes the resolved list of [Translation] objects into the [TranslationCache].
     */
    fun start()
}