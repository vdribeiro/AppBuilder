package com.app.builder.domain.usecase.translation

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.seconds
import com.app.builder.core.locale.now
import com.app.builder.data.database.reset
import com.app.builder.domain.usecase.Outcome
import com.app.builder.test.FakeData
import com.app.builder.test.TestCase

class TranslationUseCasesTest: TestCase() {

    /** Verifies that updating a stored translation changes its value and modification time, keeping the other translations unchanged. */
    @Test
    fun updateTranslation() = runServerTest {
        val translationUseCases = dependency.get()
            .useCases
            .translationUseCases
        val edited = FakeData.translations.first().copy(modifiedAt = now(), value = "edited")
        assertEquals(expected = Outcome.Success(data = Unit), actual = dependency.get().useCases.seedUseCases.seedTranslations(translations = FakeData.translations))

        assertEquals(expected = edited, actual = translationUseCases.updateTranslation(translation = edited).successData())

        assertEquals(
            expected = (FakeData.translations.drop(n = 1) + edited).toSet(),
            actual = translationUseCases.getTranslations().successData().toSet()
        )
    }

    /** Verifies that updating a translation whose language and key are not stored reports [Outcome.NotFound] and stores nothing. */
    @Test
    fun updateUnknownTranslation() = runServerTest {
        val translationUseCases = dependency.get()
            .useCases
            .translationUseCases
        val unknown = FakeData.translations.first().copy(key = "unknown_key", modifiedAt = now())
        assertEquals(expected = Outcome.Success(data = Unit), actual = dependency.get().useCases.seedUseCases.seedTranslations(translations = FakeData.translations))

        assertEquals(expected = Outcome.NotFound, actual = translationUseCases.updateTranslation(translation = unknown))
        assertEquals(expected = FakeData.translations.toSet(), actual = translationUseCases.getTranslations().successData().toSet())
    }

    /** Verifies that an update older than the stored translation is ignored and returns the stored translation. */
    @Test
    fun updateIgnoresOlderTranslation() = runServerTest {
        val translationUseCases = dependency.get()
            .useCases
            .translationUseCases
        val now = now()
        val newer = FakeData.translations.first().copy(modifiedAt = now, value = "newer")
        val older = newer.copy(modifiedAt = now - 1.seconds, value = "older")
        assertEquals(expected = Outcome.Success(data = Unit), actual = dependency.get().useCases.seedUseCases.seedTranslations(translations = FakeData.translations))
        assertEquals(expected = newer, actual = translationUseCases.updateTranslation(translation = newer).successData())

        assertEquals(expected = newer, actual = translationUseCases.updateTranslation(translation = older).successData())
    }

    /** Verifies that when the database fails, its reads and writes report [Outcome.Error] rather than an empty list, a missing translation or a refused write. */
    @Test
    fun databaseFailureIsAnError() = runServerTest {
        val translationUseCases = dependency.get()
            .useCases
            .translationUseCases
        dependency.get().database.reset()

        listOf(
            "getTranslations" to translationUseCases.getTranslations(),
            "updateTranslation" to translationUseCases.updateTranslation(translation = FakeData.translations.first())
        ).forEach { (name, outcome) -> assertEquals(expected = Outcome.Error, actual = outcome, message = name) }
    }
}
