package com.app.builder.domain.translation

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import com.app.builder.core.locale.DEFAULT_LANGUAGE
import com.app.builder.data.translation.TranslationCache
import com.app.builder.domain.Translation
import com.app.builder.test.TestCase

class TranslationServiceTest: TestCase() {

    /** In-memory [TranslationStore] fixture for feeding translations to the service under test. */
    private class FakeTranslationStore: TranslationStore {
        /** The store calls made so far, in order. */
        val calls = mutableListOf<String>()
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

        override fun observeTranslations(languageIso: String): Flow<List<Translation>> {
            calls.add(element = "observe")
            return languageFlow.map { list -> list.filter { it.languageIso == languageIso } }
        }

        override suspend fun syncTranslations() {
            calls.add(element = "sync")
        }
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

    /** Verifies that when the store has no translations for the current locale, the cache falls back to the [DEFAULT_LANGUAGE] data. */
    @Test
    fun emptyLocalDataFallsBackToDefaultLanguage() = runUnitTest {
        val store = FakeTranslationStore()
        store.setTranslations(translations = listOf(Translation(languageIso = DEFAULT_LANGUAGE, key = "hello", value = "Hello")))
        val translationService: TranslationService = TranslationManager(translationStore = store, locale = flowOf(value = PORTUGUESE))

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

    /** Verifies that starting the service syncs translations before observing them. */
    @Test
    fun startSyncsBeforeObserving() = runUnitTest {
        val store = FakeTranslationStore()
        val translationService: TranslationService = TranslationManager(translationStore = store)

        translationService.start()
        translationService.stop()

        assertEquals(expected = "sync", actual = store.calls.first())
        assertTrue(actual = "observe" in store.calls)
    }

    /** Verifies that the current locale's translations are preferred over the [DEFAULT_LANGUAGE] ones when both exist. */
    @Test
    fun currentLocaleTranslationsArePreferred() = runUnitTest {
        val store = FakeTranslationStore()
        store.setTranslations(
            translations = listOf(
                Translation(languageIso = DEFAULT_LANGUAGE, key = "hello", value = "Hello"),
                Translation(languageIso = PORTUGUESE, key = "hello", value = "Olá"),
            )
        )
        val translationService: TranslationService = TranslationManager(translationStore = store, locale = flowOf(value = PORTUGUESE))

        translationService.start()
        translationService.stop()

        assertEquals(expected = "Olá", actual = TranslationCache.get(key = "hello"))
    }

    /** Verifies that the cache falls back to the [DEFAULT_LANGUAGE] while the current locale has no translations, then switches over once they arrive. */
    @Test
    fun fallbackSwitchesToLocaleTranslationsOnceTheyArrive() = runUnitTest {
        val store = FakeTranslationStore()
        val english = Translation(languageIso = DEFAULT_LANGUAGE, key = "hello", value = "Hello")
        store.setTranslations(translations = listOf(english))
        val translationService: TranslationService = TranslationManager(translationStore = store, locale = flowOf(value = PORTUGUESE))

        translationService.start()
        assertEquals(expected = "Hello", actual = TranslationCache.get(key = "hello"))

        store.setTranslations(translations = listOf(english, Translation(languageIso = PORTUGUESE, key = "hello", value = "Olá")))
        testScheduler.runCurrent()
        translationService.stop()

        assertEquals(expected = "Olá", actual = TranslationCache.get(key = "hello"))
    }

    /** Verifies that a locale change re-resolves the translations, falling back to the [DEFAULT_LANGUAGE] for a locale without any. */
    @Test
    fun localeChangeReResolvesTranslations() = runUnitTest {
        val store = FakeTranslationStore()
        store.setTranslations(
            translations = listOf(
                Translation(languageIso = DEFAULT_LANGUAGE, key = "hello", value = "Hello"),
                Translation(languageIso = PORTUGUESE, key = "hello", value = "Olá"),
            )
        )
        val locale = MutableStateFlow(value = PORTUGUESE)
        val translationService: TranslationService = TranslationManager(translationStore = store, locale = locale)

        translationService.start()
        assertEquals(expected = "Olá", actual = TranslationCache.get(key = "hello"))

        locale.value = "fr"
        testScheduler.runCurrent()
        translationService.stop()

        assertEquals(expected = "Hello", actual = TranslationCache.get(key = "hello"))
    }

    private companion object {
        /** The language code used as the non-default locale. */
        private const val PORTUGUESE = "pt"
    }
}
