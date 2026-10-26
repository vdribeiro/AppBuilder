package com.app.builder.domain.gateway.translation

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlin.uuid.Uuid
import kotlinx.coroutines.flow.firstOrNull
import app.cash.sqldelight.async.coroutines.awaitAsList
import com.app.builder.core.config.ClientFlags
import com.app.builder.core.locale.DEFAULT_LANGUAGE
import com.app.builder.domain.EntityType
import com.app.builder.domain.Translation
import com.app.builder.domain.scheduler.Job
import com.app.builder.domain.scheduler.JobResult
import com.app.builder.test.FakeData
import com.app.builder.test.TestCase

class TranslationGatewayTest: TestCase() {

    /** Verifies that syncing translations populates the observable translations for the default language. */
    @Test
    fun syncAndObserveTranslations() = runUnitTest {
        val translationUseCases = dependency.get()
            .userGraph
            .repositories
            .translationRepository

        assertTrue(actual = translationUseCases.observeTranslations(languageIso = DEFAULT_LANGUAGE).firstOrNull().orEmpty().isEmpty())
        translationUseCases.syncTranslations()
        assertFalse(actual = translationUseCases.observeTranslations(languageIso = DEFAULT_LANGUAGE).firstOrNull().orEmpty().isEmpty())
    }

    /** Verifies that executing a translations sync job succeeds and populates the translation table. */
    @Test
    fun executeSyncTranslations() = runUnitTest {
        val translationUseCases = dependency.get()
            .userGraph
            .repositories
            .translationRepository
        val translationDao = dependency.get()
            .database
            .translationQueries

        assertTrue(actual = translationDao.isTranslationEmpty().awaitAsList().isEmpty())

        val jobResult = translationUseCases.executeSyncTranslations(job = Job(userUuid = Uuid.NIL, entityType = EntityType.TRANSLATION, type = Job.Type.GET))
        assertTrue(actual = jobResult is JobResult.Success)

        assertFalse(actual = translationDao.isTranslationEmpty().awaitAsList().isEmpty())
    }

    /** Verifies that executing a translations sync job fails when the http client flag is disabled. */
    @Test
    fun executeSyncTranslationsOffline() = runUnitTest {
        ClientFlags.set { it.copy(http = false) }
        val translationUseCases = dependency.get()
            .userGraph
            .repositories
            .translationRepository

        val jobResult = translationUseCases.executeSyncTranslations(job = Job(userUuid = Uuid.NIL, entityType = EntityType.TRANSLATION, type = Job.Type.GET))
        assertTrue(actual = jobResult is JobResult.Error)
    }

    /** Verifies that a translations sync job replaces every stored translation with the fetched ones, filtered by language when observed. */
    @Test
    fun executeSyncTranslationsRewrites() = runUnitTest {
        val translationRepository = dependency.get()
            .userGraph
            .repositories
            .translationRepository
        val translationDao = dependency.get()
            .database
            .translationQueries

        val stale = Translation(languageIso = "en", key = "stale", value = "Stale")
        translationDao.upsertTranslation(Translation = stale.toTranslationSchema())

        val jobResult = translationRepository.executeSyncTranslations(job = Job(userUuid = Uuid.NIL, entityType = EntityType.TRANSLATION, type = Job.Type.GET))
        assertTrue(actual = jobResult is JobResult.Success)

        assertEquals(
            expected = FakeData.translations.filter { it.languageIso == "en" }.toSet(),
            actual = translationRepository.observeTranslations(languageIso = "en").firstOrNull().orEmpty().toSet()
        )
        assertEquals(
            expected = FakeData.translations.filter { it.languageIso == "pt" }.toSet(),
            actual = translationRepository.observeTranslations(languageIso = "pt").firstOrNull().orEmpty().toSet()
        )
    }

    /** Verifies that syncing translations keeps already stored translations instead of prepopulating over them, and queues a remote sync. */
    @Test
    fun syncTranslationsKeepsStored() = runUnitTest {
        val translationRepository = dependency.get()
            .userGraph
            .repositories
            .translationRepository
        val translationDao = dependency.get()
            .database
            .translationQueries
        val jobDao = dependency.get()
            .database
            .jobQueries

        val stored = Translation(languageIso = DEFAULT_LANGUAGE, key = "stored", value = "Stored")
        translationDao.upsertTranslation(Translation = stored.toTranslationSchema())

        translationRepository.syncTranslations()

        assertEquals(expected = listOf(element = stored), actual = translationRepository.observeTranslations(languageIso = DEFAULT_LANGUAGE).firstOrNull())
        assertNotNull(actual = jobDao.getJobsByState(states = Job.State.entries).awaitAsList().find { it.entityType == EntityType.TRANSLATION && it.type == Job.Type.GET })
    }
}
