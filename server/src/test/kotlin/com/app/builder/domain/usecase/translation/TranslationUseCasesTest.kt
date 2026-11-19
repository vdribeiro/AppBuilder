package com.app.builder.domain.usecase.translation

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import com.app.builder.core.locale.now
import com.app.builder.data.database.reset
import com.app.builder.domain.usecase.Outcome
import com.app.builder.test.FakeData
import com.app.builder.test.TestCase

class TranslationUseCasesTest: TestCase() {

    /** Verifies that seeded translations are retrievable sorted by language and key. */
    @Test
    fun seedAndGetTranslations() = runServerTest {
        val translationUseCases = dependency.get()
            .useCases
            .translationUseCases

        assertTrue(actual = translationUseCases.getTranslations().successData().isEmpty())
        assertEquals(expected = Outcome.Success(data = Unit), actual = translationUseCases.seedTranslations(translations = FakeData.translations))
        assertEquals(
            expected = FakeData.translations.sortedWith(comparator = compareBy({ it.languageIso }, { it.key })),
            actual = translationUseCases.getTranslations().successData()
        )
    }

    /** Verifies that seeding inserts only the translations that are not stored yet, keeping the stored ones unchanged. */
    @Test
    fun seedKeepsStoredTranslations() = runServerTest {
        val translationUseCases = dependency.get()
            .useCases
            .translationUseCases
        val stored = FakeData.translations.first().copy(modifiedAt = now(), value = "edited")

        assertEquals(expected = Outcome.Success(data = Unit), actual = translationUseCases.seedTranslations(translations = listOf(element = stored)))
        assertEquals(expected = Outcome.Success(data = Unit), actual = translationUseCases.seedTranslations(translations = FakeData.translations))

        val translations = translationUseCases.getTranslations().successData()
        assertEquals(expected = FakeData.translations.size, actual = translations.size)
        assertEquals(expected = stored, actual = translations.single { it.languageIso == stored.languageIso && it.key == stored.key })
    }

    /** Verifies that seeding a list holding the same language and key twice stores the first one. */
    @Test
    fun seedIgnoresDuplicates() = runServerTest {
        val translationUseCases = dependency.get()
            .useCases
            .translationUseCases
        val translation = FakeData.translations.first()

        assertEquals(
            expected = Outcome.Success(data = Unit),
            actual = translationUseCases.seedTranslations(translations = listOf(translation, translation.copy(value = "duplicate")))
        )
        assertEquals(expected = listOf(element = translation), actual = translationUseCases.getTranslations().successData())
    }

    /** Verifies that when the database fails, its reads and writes report [Outcome.Error] rather than an empty list or a refused write. */
    @Test
    fun databaseFailureIsAnError() = runServerTest {
        val translationUseCases = dependency.get()
            .useCases
            .translationUseCases
        dependency.get().database.reset()

        listOf(
            "seedTranslations" to translationUseCases.seedTranslations(translations = FakeData.translations),
            "getTranslations" to translationUseCases.getTranslations()
        ).forEach { (name, outcome) -> assertEquals(expected = Outcome.Error, actual = outcome, message = name) }
    }
}
