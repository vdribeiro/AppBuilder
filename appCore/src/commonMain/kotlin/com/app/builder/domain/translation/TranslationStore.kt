package com.app.builder.domain.translation

import kotlinx.coroutines.flow.Flow
import com.app.builder.domain.Translation

/** Persistence contract for observing localized translation data. */
interface TranslationStore {

    /**
     * Observes translations matching the provided language code.
     * This function returns a [Flow] that automatically emits a new list of [Translation] objects whenever the underlying data source updates.
     *
     * @param languageIso The ISO language code used to filter the translations.
     * @return A [Flow] emitting the current list of available translations for the specified language.
     */
    fun observeTranslations(languageIso: String): Flow<List<Translation>>

    /** Syncs translations. */
    suspend fun syncTranslations()
}
