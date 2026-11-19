package com.app.builder.domain.gateway.translation

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.seconds
import kotlin.uuid.Uuid
import kotlinx.coroutines.flow.firstOrNull
import app.cash.sqldelight.async.coroutines.awaitAsList
import com.app.builder.Dependency.getUserDependency
import com.app.builder.core.config.ClientFlags
import com.app.builder.core.locale.DEFAULT_LANGUAGE
import com.app.builder.core.locale.SelectedLanguage
import com.app.builder.core.locale.now
import com.app.builder.data.serializer.encode
import com.app.builder.data.storage.CoreFile
import com.app.builder.domain.EntityType
import com.app.builder.domain.Translation
import com.app.builder.domain.scheduler.Job
import com.app.builder.domain.scheduler.JobResult
import com.app.builder.test.FakeData
import com.app.builder.test.TestCase

class TranslationGatewayTest: TestCase() {

    override suspend fun beforeTest() {
        super.beforeTest()
        ClientFlags.set { it.copy(http = true, scheduler = true) }
    }

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

    /** Verifies that a translations sync job deletes the stored translations that were not fetched and stores the fetched ones, filtered by language when observed. */
    @Test
    fun executeSyncTranslationsDeletesUnknown() = runUnitTest {
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

    /** Verifies that the observed languages are the distinct languages holding translations, sorted alphabetically. */
    @Test
    fun observeLanguages() = runUnitTest {
        val translationUseCases = dependency.get()
            .userGraph
            .useCases
            .translationUseCases
        val translationDao = dependency.get()
            .database
            .translationQueries

        assertEquals(expected = emptyList(), actual = translationUseCases.observeLanguages().firstOrNull())

        listOf(
            Translation(languageIso = "pt", key = "yes", value = "Sim"),
            Translation(languageIso = "en", key = "yes", value = "Yes"),
            Translation(languageIso = "en", key = "no", value = "No"),
        ).forEach { translationDao.upsertTranslation(Translation = it.toTranslationSchema()) }

        assertEquals(expected = listOf("en", "pt"), actual = translationUseCases.observeLanguages().firstOrNull())
    }

    /** Verifies that a translations sync job keeps a stored translation that is newer than the fetched one. */
    @Test
    fun executeSyncTranslationsKeepsNewer() = runUnitTest {
        val translationRepository = dependency.get()
            .userGraph
            .repositories
            .translationRepository
        val translationDao = dependency.get()
            .database
            .translationQueries

        val edited = FakeData.translations.first().copy(modifiedAt = now(), value = "edited")
        translationDao.upsertTranslation(Translation = edited.toTranslationSchema())

        val jobResult = translationRepository.executeSyncTranslations(job = Job(userUuid = Uuid.NIL, entityType = EntityType.TRANSLATION, type = Job.Type.GET))
        assertTrue(actual = jobResult is JobResult.Success)

        assertEquals(
            expected = (FakeData.translations.drop(n = 1) + edited).filter { it.languageIso == edited.languageIso }.toSet(),
            actual = translationRepository.observeTranslations(languageIso = edited.languageIso).firstOrNull().orEmpty().toSet()
        )
    }

    /** Verifies that updating a stored translation changes its value and modification time and queues its upload. */
    @Test
    fun updateTranslation() = runUnitTest {
        val translationUseCases = dependency.get()
            .getUserDependency(account = FakeData.adminAccount)
            .useCases
            .translationUseCases
        val translationDao = dependency.get()
            .database
            .translationQueries

        val stored = FakeData.translations.first()
        val edited = stored.copy(modifiedAt = now(), value = "edited")
        translationDao.upsertTranslation(Translation = stored.toTranslationSchema())

        translationUseCases.updateTranslation(translation = edited)

        assertEquals(expected = listOf(element = edited), actual = translationUseCases.observeTranslations(languageIso = stored.languageIso).firstOrNull())
        assertEquals(expected = encode(value = edited), actual = assertNotNull(actual = queuedUpdateJob()).payload)
    }

    /** Verifies that updating a translation whose language and key are not stored stores nothing and queues no upload. */
    @Test
    fun updateUnknownTranslation() = runUnitTest {
        val translationUseCases = dependency.get()
            .getUserDependency(account = FakeData.adminAccount)
            .useCases
            .translationUseCases

        val unknown = FakeData.translations.first().copy(modifiedAt = now(), value = "edited")
        translationUseCases.updateTranslation(translation = unknown)

        assertEquals(expected = emptyList(), actual = translationUseCases.observeTranslations(languageIso = unknown.languageIso).firstOrNull())
        assertNull(actual = queuedUpdateJob())
    }

    /** Verifies that updating a translation without translation write permission keeps the stored translation and queues no upload. */
    @Test
    fun updateTranslationRequiresPermission() = runUnitTest {
        // FakeData.account only has TRANSLATION -> READ.
        val translationUseCases = dependency.get()
            .getUserDependency(account = FakeData.account)
            .useCases
            .translationUseCases
        val translationDao = dependency.get()
            .database
            .translationQueries

        val stored = FakeData.translations.first()
        translationDao.upsertTranslation(Translation = stored.toTranslationSchema())

        translationUseCases.updateTranslation(translation = stored.copy(modifiedAt = now(), value = "edited"))

        assertEquals(expected = listOf(element = stored), actual = translationUseCases.observeTranslations(languageIso = stored.languageIso).firstOrNull())
        assertNull(actual = queuedUpdateJob())
    }

    /** Verifies that executing an update translation job fails without a payload and succeeds once the translation is encoded into it, storing the answered translation. */
    @Test
    fun executeUpdateTranslation() = runUnitTest {
        val translationRepository = dependency.get()
            .getUserDependency(account = FakeData.adminAccount)
            .repositories
            .translationRepository
        val translationDao = dependency.get()
            .database
            .translationQueries

        val stored = FakeData.translations.first()
        val edited = stored.copy(modifiedAt = now(), value = "edited")
        translationDao.upsertTranslation(Translation = stored.toTranslationSchema())

        val job = Job(userUuid = Uuid.NIL, entityType = EntityType.TRANSLATION, type = Job.Type.POST)
        assertTrue(actual = translationRepository.executeUpdateTranslation(job = job) is JobResult.Error)

        val jobResult = translationRepository.executeUpdateTranslation(job = job.copy(payload = encode(value = edited)))
        assertTrue(actual = jobResult is JobResult.Success)

        assertEquals(expected = listOf(element = edited), actual = translationRepository.observeTranslations(languageIso = stored.languageIso).firstOrNull())
    }

    /** Verifies that executing an update translation job keeps a stored translation that is newer than the answered one. */
    @Test
    fun executeUpdateTranslationKeepsNewer() = runUnitTest {
        val translationRepository = dependency.get()
            .getUserDependency(account = FakeData.adminAccount)
            .repositories
            .translationRepository
        val translationDao = dependency.get()
            .database
            .translationQueries

        val now = now()
        val older = FakeData.translations.first().copy(modifiedAt = now - 1.seconds, value = "older")
        val newer = older.copy(modifiedAt = now, value = "newer")
        translationDao.upsertTranslation(Translation = newer.toTranslationSchema())

        val job = Job(userUuid = Uuid.NIL, entityType = EntityType.TRANSLATION, type = Job.Type.POST, payload = encode(value = older))
        assertTrue(actual = translationRepository.executeUpdateTranslation(job = job) is JobResult.Success)

        assertEquals(expected = listOf(element = newer), actual = translationRepository.observeTranslations(languageIso = newer.languageIso).firstOrNull())
    }

    /** Verifies that executing an update translation job fails when the http client flag is disabled. */
    @Test
    fun executeUpdateTranslationOffline() = runUnitTest {
        ClientFlags.set { it.copy(http = false) }
        val translationRepository = dependency.get()
            .getUserDependency(account = FakeData.adminAccount)
            .repositories
            .translationRepository

        val job = Job(
            userUuid = Uuid.NIL,
            entityType = EntityType.TRANSLATION,
            type = Job.Type.POST,
            payload = encode(value = FakeData.translations.first())
        )
        assertTrue(actual = translationRepository.executeUpdateTranslation(job = job) is JobResult.Error)
    }

    /** Verifies that executing an update translation job fails without translation write permission. */
    @Test
    fun executeUpdateTranslationRequiresPermission() = runUnitTest {
        // FakeData.account only has TRANSLATION -> READ.
        val translationRepository = dependency.get()
            .getUserDependency(account = FakeData.account)
            .repositories
            .translationRepository

        val job = Job(
            userUuid = Uuid.NIL,
            entityType = EntityType.TRANSLATION,
            type = Job.Type.POST,
            payload = encode(value = FakeData.translations.first())
        )
        assertTrue(actual = translationRepository.executeUpdateTranslation(job = job) is JobResult.Error)
    }

    /** Verifies that selecting a language saves it and sets it as the selected language, and that selecting none removes it and keeps the other device values. */
    @Test
    fun selectLanguage() = runUnitTest {
        val translationUseCases = dependency.get()
            .userGraph
            .useCases
            .translationUseCases
        CoreFile.Device.save { it.orEmpty().plus(pair = (CoreFile.Device.Key.CLOCK_OFFSET to "1")) }

        translationUseCases.selectLanguage(languageIso = "pt")

        assertEquals(expected = "pt", actual = SelectedLanguage.value)
        assertEquals(expected = "pt", actual = CoreFile.Device.load().orEmpty()[CoreFile.Device.Key.LANGUAGE])

        translationUseCases.selectLanguage(languageIso = null)

        assertNull(actual = SelectedLanguage.value)
        assertNull(actual = CoreFile.Device.load().orEmpty()[CoreFile.Device.Key.LANGUAGE])
        assertEquals(expected = "1", actual = CoreFile.Device.load().orEmpty()[CoreFile.Device.Key.CLOCK_OFFSET])
    }

    /**
     * Finds the queued job that uploads a translation, in any state.
     *
     * @return The queued job, or `null` if none is queued.
     */
    private suspend fun queuedUpdateJob(): database.Job? = dependency.get()
        .database
        .jobQueries
        .getJobsByState(states = Job.State.entries)
        .awaitAsList()
        .find { it.entityType == EntityType.TRANSLATION && it.type == Job.Type.POST }
}
