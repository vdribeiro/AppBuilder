package com.app.builder.domain.usecase.translation

import com.app.builder.domain.Translation
import com.app.builder.domain.usecase.Outcome

/** Translation use cases. */
interface TranslationUseCases {

    /**
     * Inserts the translations whose language and key are not stored yet.
     *
     * @param translations The translations to seed.
     * @return [Outcome.Success] when persisted, or [Outcome.Error] when the operation failed.
     */
    suspend fun seedTranslations(translations: List<Translation>): Outcome<Unit>

    /**
     * Retrieves every translation.
     *
     * @return [Outcome.Success] with the translations sorted by language and key, or [Outcome.Error] when the operation failed.
     */
    suspend fun getTranslations(): Outcome<List<Translation>>
}
