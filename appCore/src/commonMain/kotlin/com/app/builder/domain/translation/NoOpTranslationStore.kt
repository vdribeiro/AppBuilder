package com.app.builder.domain.translation

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import com.app.builder.domain.Translation

/** No-op implementation of [TranslationStore]. */
class NoOpTranslationStore: TranslationStore {

    override fun observeTranslations(languageIso: String): Flow<List<Translation>> = emptyFlow()

    override suspend fun syncTranslations() {}
}
