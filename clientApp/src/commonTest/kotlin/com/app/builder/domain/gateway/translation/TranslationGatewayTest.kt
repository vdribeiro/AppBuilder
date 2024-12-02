package com.app.builder.domain.gateway.translation

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.uuid.Uuid
import kotlinx.coroutines.flow.firstOrNull
import app.cash.sqldelight.async.coroutines.awaitAsList
import com.app.builder.core.config.ClientFlags
import com.app.builder.core.locale.DEFAULT_LANGUAGE
import com.app.builder.domain.EntityType
import com.app.builder.domain.scheduler.Job
import com.app.builder.domain.scheduler.JobResult
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
}
