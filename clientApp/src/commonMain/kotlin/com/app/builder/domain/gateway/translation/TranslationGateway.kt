package com.app.builder.domain.gateway.translation

import kotlin.uuid.Uuid
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import io.ktor.client.HttpClient
import app.cash.sqldelight.async.coroutines.awaitAsList
import app.cash.sqldelight.async.coroutines.awaitAsOneOrNull
import com.app.builder.core.flow.Dispatcher
import com.app.builder.core.flow.runSuspendCatching
import com.app.builder.core.locale.SelectedLanguage
import com.app.builder.core.telemetry.Telemetry
import com.app.builder.data.database.asFlow
import com.app.builder.data.database.safeTransaction
import com.app.builder.data.http.HttpRequest
import com.app.builder.data.http.HttpResult
import com.app.builder.data.http.URL
import com.app.builder.data.http.get
import com.app.builder.data.http.post
import com.app.builder.data.resource.JsonResource
import com.app.builder.data.resource.loadResource
import com.app.builder.data.serializer.decode
import com.app.builder.data.serializer.encode
import com.app.builder.data.storage.CoreFile
import com.app.builder.domain.Account
import com.app.builder.domain.EntityType
import com.app.builder.domain.Permission
import com.app.builder.domain.Translation
import com.app.builder.domain.gateway.account.validatePermission
import com.app.builder.domain.scheduler.Job
import com.app.builder.domain.scheduler.JobResult
import com.app.builder.domain.scheduler.JobResult.Success.toJobResult
import com.app.builder.domain.scheduler.Scheduler
import com.app.builder.domain.scheduler.toHeaderMap
import database.AppDatabase

/**
 * Translation repository.
 *
 * @property account The account of the current user.
 * @property database The SQLite database instance for translation data.
 * @property httpClient The HTTP client used for network operations.
 * @property scheduler The job scheduler for background tasks.
 */
class TranslationGateway(
    private val account: Account,
    private val database: AppDatabase,
    private val httpClient: HttpClient,
    private val scheduler: Scheduler
): TranslationRepository, TranslationUseCases {

    /** Query interface for the translations table. */
    private val translationDao = database.translationQueries

    override fun observeLanguages(): Flow<List<String>> =
        translationDao.getLanguages()
            .asFlow { it }
            .flowOn(context = Dispatcher.IO)
            .catch {
                Telemetry.error(tag = TAG, message = "Unable to observe languages", throwable = it)
                emit(value = emptyList())
            }

    override fun observeTranslations(languageIso: String): Flow<List<Translation>> =
        translationDao.getTranslations(languageIso = languageIso)
            .asFlow { it.toTranslation() }
            .flowOn(context = Dispatcher.IO)
            .catch {
                Telemetry.error(tag = TAG, message = "Unable to observe translations", throwable = it)
                emit(value = emptyList())
            }

    override suspend fun updateTranslation(translation: Translation) = withContext(context = Dispatcher.IO) {
        account.validatePermission(tag = TAG, entityType = EntityType.TRANSLATION, permission = Permission.WRITE)?.let { return@withContext }

        val updated = database.safeTransaction {
            updateTranslationInDatabase(translation = translation)
        }.onFailure {
            Telemetry.error(tag = TAG, message = "Unable to update translation", throwable = it)
        }.getOrDefault(defaultValue = false)
        if (!updated) return@withContext

        val job = Job(
            userUuid = account.userUuid,
            entityType = EntityType.TRANSLATION,
            type = Job.Type.POST,
            payload = encode(value = translation)
        )
        scheduler.queue(job = job)
    }

    override suspend fun selectLanguage(languageIso: String?) = withContext(context = Dispatcher.IO) {
        val saved = CoreFile.Device.save { device ->
            when (languageIso) {
                null -> device.orEmpty().minus(key = CoreFile.Device.Key.LANGUAGE)
                else -> device.orEmpty().plus(pair = (CoreFile.Device.Key.LANGUAGE to languageIso))
            }
        }
        if (!saved) return@withContext Telemetry.error(tag = TAG, message = "Unable to save language $languageIso")

        SelectedLanguage.set { languageIso }
        Unit
    }

    override suspend fun syncTranslations() = withContext(context = Dispatcher.IO) {
        val isEmpty = runSuspendCatching { isTranslationEmpty() }
            .onFailure { Telemetry.error(tag = TAG, message = "Unable to check translations", throwable = it) }
            .getOrDefault(defaultValue = false)
        if (isEmpty) {
            val translations: List<Translation> = loadResource<Translation>(json = JsonResource.Translations)
            database.safeTransaction {
                rewriteTranslations(translations = translations)
            }.onSuccess {
                Telemetry.info(tag = TAG, message = "Prepopulated translations")
            }.onFailure {
                Telemetry.error(tag = TAG, message = "Unable to rewrite translations", throwable = it)
            }
        }
        val job = Job(
            userUuid = Uuid.NIL,
            entityType = EntityType.TRANSLATION,
            type = Job.Type.GET,
        )
        scheduler.queue(job = job)
    }

    override suspend fun executeSyncTranslations(job: Job): JobResult = withContext(context = Dispatcher.IO) {
        when (val result = httpClient.get<List<Translation>>(
            request = HttpRequest(
                url = URL.Translations,
                headerMap = job.toHeaderMap(),
            )
        )) {
            is HttpResult.Error -> {
                Telemetry.error(tag = TAG, message = "Unable to get remote translations", throwable = result.error)
                result.error.toJobResult()
            }

            is HttpResult.Success -> {
                database.safeTransaction {
                    syncTranslationsInDatabase(translations = result.data)
                }.onFailure {
                    Telemetry.error(tag = TAG, message = "Unable to sync translations", throwable = it)
                    return@withContext it.toJobResult()
                }
                Telemetry.info(tag = TAG, message = "Successfully fetched translations")
                JobResult.Success
            }
        }
    }

    override suspend fun executeUpdateTranslation(job: Job): JobResult = withContext(context = Dispatcher.IO) {
        account.validatePermission(tag = TAG, entityType = EntityType.TRANSLATION, permission = Permission.WRITE)?.let { return@withContext it.toJobResult() }

        val translation = job.payload?.let { decode<Translation>(value = it) } ?: run {
            val throwable = Throwable(message = "Unable to decode translation")
            Telemetry.error(tag = TAG, message = "Invalid translation payload", throwable = throwable)
            return@withContext throwable.toJobResult()
        }

        when (val result = httpClient.post<Translation, Translation>(
            request = HttpRequest(
                url = URL.Translations,
                headerMap = job.toHeaderMap(),
            ),
            body = translation
        )) {
            is HttpResult.Error -> {
                Telemetry.error(tag = TAG, message = "Unable to post translation", throwable = result.error)
                result.error.toJobResult()
            }

            is HttpResult.Success -> {
                database.safeTransaction {
                    syncTranslationInDatabase(translation = result.data)
                }.onFailure {
                    Telemetry.error(tag = TAG, message = "Unable to sync translation", throwable = it)
                    return@withContext it.toJobResult()
                }
                Telemetry.info(tag = TAG, message = "Successfully updated translation")
                JobResult.Success
            }
        }
    }

    /**
     * Stores a remote translation, unless the local translation with the same language and key is newer.
     *
     * @param translation The remote [Translation] to store.
     */
    private suspend fun syncTranslationInDatabase(translation: Translation) {
        val localTranslation = translationDao.getTranslation(languageIso = translation.languageIso, key = translation.key).awaitAsOneOrNull()
        if (localTranslation == null || translation.modifiedAt >= localTranslation.modifiedAt) upsertTranslationInDatabase(translation = translation)
    }

    /**
     * Deletes the local translations whose language and key are not among the remote ones, then stores each remote translation, unless the local translation with the same language and key is newer.
     *
     * @param translations The remote [Translation] objects to store.
     */
    private suspend fun syncTranslationsInDatabase(translations: List<Translation>) {
        val localTranslationsMap = translationDao.getAllTranslations().awaitAsList().associateBy { it.languageIso to it.key }
        val remoteKeys = translations.map { it.languageIso to it.key }.toSet()
        localTranslationsMap.keys
            .filter { it !in remoteKeys }
            .forEach { (languageIso, key) -> translationDao.deleteTranslation(languageIso = languageIso, key = key) }
        translations.forEach { translation ->
            val localTranslation = localTranslationsMap[translation.languageIso to translation.key]
            if (localTranslation == null || translation.modifiedAt >= localTranslation.modifiedAt) upsertTranslationInDatabase(translation = translation)
        }
    }

    /**
     * Updates the value and modification time of a translation in the local database.
     *
     * @param translation The [Translation] holding the new value, identified by its language and key.
     * @return `true` if the translation was updated; `false` if its language and key are not stored.
     */
    private suspend fun updateTranslationInDatabase(translation: Translation): Boolean =
        translationDao.updateTranslation(
            modifiedAt = translation.modifiedAt,
            value = translation.value,
            languageIso = translation.languageIso,
            key = translation.key
        ) > 0

    /**
     * Inserts or updates a translation in the local database.
     *
     * @param translation The [Translation] to save.
     * @return `true` if the translation was successfully upserted; `false` if no rows were affected.
     */
    private suspend fun upsertTranslationInDatabase(translation: Translation): Boolean =
        translationDao.upsertTranslation(Translation = translation.toTranslationSchema()) > 0

    /**
     * Checks whether the local database currently contains any translations.
     *
     * @return `true` if the database is confirmed to have zero translations. Returns `false` if translations exist.
     */
    private suspend fun isTranslationEmpty(): Boolean =
        translationDao.isTranslationEmpty().awaitAsList().isEmpty()

    /**
     * Replaces the entirety of the local translation database with a new dataset.
     *
     * @param translations The new list of [Translation] objects to persist.
     */
    private suspend fun rewriteTranslations(translations: List<Translation>) {
        translationDao.truncateTranslation()
        translations.forEach { upsertTranslationInDatabase(translation = it) }
    }

    companion object {
        private const val TAG = "TranslationsGateway"
    }
}
