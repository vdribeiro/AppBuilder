package com.app.builder.domain.translation

import java.util.Locale
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import com.app.builder.core.locale.DEFAULT_LANGUAGE
import com.app.builder.data.translation.TranslationCache
import com.app.builder.domain.Translation
import com.app.builder.test.TestCase

/** Desktop-specific coverage for [TranslationManager]'s locale resolution, which requires mutating the real JVM default locale, only meaningfully controllable on this target. */
class TranslationServiceDesktopTest: TestCase() {

    /** In-memory [TranslationStore] fixture holding translations for any number of languages. */
    private class FakeTranslationStore: TranslationStore {
        /** Every translation held by the store, across languages. */
        val translations = MutableStateFlow<List<Translation>>(value = emptyList())

        override fun observeTranslations(languageIso: String): Flow<List<Translation>> =
            translations.map { list -> list.filter { it.languageIso == languageIso } }

        override suspend fun syncTranslations() {}
    }

    /**
     * Runs [block] with the JVM default locale set to [languageTag], restoring the original locale afterwards.
     *
     * @param languageTag The IETF language tag to use as the default locale.
     * @param block The code to run under that locale.
     */
    private inline fun withLocale(languageTag: String, block: () -> Unit) {
        val originalLocale = Locale.getDefault()
        try {
            Locale.setDefault(Locale.forLanguageTag(languageTag))
            block()
        } finally {
            Locale.setDefault(originalLocale)
        }
    }

    /** Verifies that the current locale's translations are preferred over the [DEFAULT_LANGUAGE] ones when both exist. */
    @Test
    fun currentLocaleTranslationsArePreferred() = runUnitTest {
        withLocale(languageTag = PORTUGUESE) {
            val store = FakeTranslationStore()
            store.translations.value = listOf(
                Translation(languageIso = DEFAULT_LANGUAGE, key = KEY, value = "Hello"),
                Translation(languageIso = PORTUGUESE, key = KEY, value = "Olá"),
            )
            val translationService: TranslationService = TranslationManager(translationStore = store)

            translationService.start()
            translationService.stop()

            assertEquals(expected = "Olá", actual = TranslationCache.get(key = KEY))
        }
    }

    /** Verifies that the cache falls back to the [DEFAULT_LANGUAGE] while the current locale has no translations, then switches over once they arrive. */
    @Test
    fun fallbackSwitchesToLocaleTranslationsOnceTheyArrive() = runUnitTest {
        withLocale(languageTag = PORTUGUESE) {
            val store = FakeTranslationStore()
            store.translations.value = listOf(Translation(languageIso = DEFAULT_LANGUAGE, key = KEY, value = "Hello"))
            val translationService: TranslationService = TranslationManager(translationStore = store)

            translationService.start()
            assertEquals(expected = "Hello", actual = TranslationCache.get(key = KEY))

            store.translations.update { it + Translation(languageIso = PORTUGUESE, key = KEY, value = "Olá") }
            testScheduler.runCurrent()
            translationService.stop()

            assertEquals(expected = "Olá", actual = TranslationCache.get(key = KEY))
        }
    }

    /** Verifies that a locale change re-resolves the translations, falling back to the [DEFAULT_LANGUAGE] for a locale without any. */
    @Test
    fun localeChangeReResolvesTranslations() = runUnitTest {
        withLocale(languageTag = PORTUGUESE) {
            val store = FakeTranslationStore()
            store.translations.value = listOf(
                Translation(languageIso = DEFAULT_LANGUAGE, key = KEY, value = "Hello"),
                Translation(languageIso = PORTUGUESE, key = KEY, value = "Olá"),
            )
            val translationService: TranslationService = TranslationManager(translationStore = store)

            translationService.start()
            assertEquals(expected = "Olá", actual = TranslationCache.get(key = KEY))

            Locale.setDefault(Locale.forLanguageTag("fr-FR"))
            testScheduler.advanceTimeBy(delayTimeMillis = INTERVAL_MILLIS)
            testScheduler.runCurrent()
            translationService.stop()

            assertEquals(expected = "Hello", actual = TranslationCache.get(key = KEY))
        }
    }

    private companion object {
        /** The language tag fixture used as the non-default locale. */
        private const val PORTUGUESE = "pt-PT"
        /** The translation key fixture. */
        private const val KEY = "greeting"
        /** The interval, in milliseconds, matching the desktop locale-change loop's default polling interval. */
        private const val INTERVAL_MILLIS = 3000L
    }
}
