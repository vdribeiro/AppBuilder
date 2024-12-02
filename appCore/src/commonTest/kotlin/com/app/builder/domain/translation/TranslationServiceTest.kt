package com.app.builder.domain.translation

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import com.app.builder.core.locale.DEFAULT_LANGUAGE
import com.app.builder.data.translation.TranslationCache
import com.app.builder.domain.Translation
import com.app.builder.test.TestCase

class TranslationServiceTest: TestCase() {

    /** In-memory [TranslationStore] fixture for feeding translations to the service under test. */
    private class FakeTranslationStore: TranslationStore {
        /** Backing flow of translations exposed by [observeTranslations]. */
        private val languageFlow = MutableStateFlow<List<Translation>>(value = emptyList())

        /**
         * Replaces the translations emitted to observers.
         *
         * @param translations The translations to emit.
         */
        fun setTranslations(translations: List<Translation>) {
            languageFlow.value = translations
        }

        override fun observeTranslations(languageIso: String): Flow<List<Translation>> =
            languageFlow.map { list -> list.filter { it.languageIso == languageIso } }

        override suspend fun syncTranslations() {}
    }

    /** Verifies starting the service populates the translation cache from the store. */
    @Test
    fun startPopulatesCache() = runUnitTest {
        val store = FakeTranslationStore()
        store.setTranslations(translations = listOf(Translation(languageIso = DEFAULT_LANGUAGE, key = "hello", value = "Hello")))
        val translationService: TranslationService = TranslationManager(translationStore = store)

        assertTrue(actual = TranslationCache.isEmpty())
        translationService.start()
        translationService.stop()
        assertTrue(actual = !TranslationCache.isEmpty())
    }

    /**
     * Verifies that when the store has no translations for the current locale, the cache falls back to the [DEFAULT_LANGUAGE] data.
     * On virtually every real device/CI locale this exercises the fallback branch; on the rare environment whose locale is exactly [DEFAULT_LANGUAGE], the assertion still holds since the primary lookup already returns the same data.
     */
    @Test
    fun emptyLocalDataFallsBackToDefaultLanguage() = runUnitTest {
        val store = FakeTranslationStore()
        store.setTranslations(translations = listOf(Translation(languageIso = DEFAULT_LANGUAGE, key = "hello", value = "Hello")))
        val translationService: TranslationService = TranslationManager(translationStore = store)

        translationService.start()
        translationService.stop()

        assertEquals(expected = "Hello", actual = TranslationCache.get(key = "hello"))
    }

    /** Verifies that stopping the service halts further cache updates from the store. */
    @Test
    fun stopHaltsFurtherCacheUpdates() = runUnitTest {
        val store = FakeTranslationStore()
        val translationService: TranslationService = TranslationManager(translationStore = store)

        translationService.start()
        translationService.stop()

        store.setTranslations(translations = listOf(Translation(languageIso = DEFAULT_LANGUAGE, key = "late", value = "Too late")))

        assertEquals(expected = "late", actual = TranslationCache.get(key = "late"))
    }

    /** Verifies that calling start() a second time while already running is a safe no-op. */
    @Test
    fun startTwiceIsIdempotent() = runUnitTest {
        val store = FakeTranslationStore()
        store.setTranslations(translations = listOf(Translation(languageIso = DEFAULT_LANGUAGE, key = "hello", value = "Hello")))
        val translationService: TranslationService = TranslationManager(translationStore = store)

        translationService.start()
        translationService.start()
        translationService.stop()

        assertEquals(expected = "Hello", actual = TranslationCache.get(key = "hello"))
    }
}
