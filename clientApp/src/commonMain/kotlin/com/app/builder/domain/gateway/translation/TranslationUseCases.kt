package com.app.builder.domain.gateway.translation

import kotlinx.coroutines.flow.Flow
import com.app.builder.domain.Translation

/** Defines the business workflows for browsing and editing the stored translations. */
interface TranslationUseCases {

    /**
     * Observes the languages that hold at least one translation.
     *
     * @return A [Flow] emitting the ISO language codes, sorted alphabetically.
     */
    fun observeLanguages(): Flow<List<String>>

    /**
     * Observes translations matching the provided language code.
     *
     * @param languageIso The ISO language code used to filter the translations.
     * @return A [Flow] emitting the current list of translations for the specified language.
     */
    fun observeTranslations(languageIso: String): Flow<List<Translation>>

    /**
     * Updates the value and modification time of a stored translation and queues its upload.
     *
     * @param translation The translation to update, identified by its language and key.
     */
    suspend fun updateTranslation(translation: Translation)
}
