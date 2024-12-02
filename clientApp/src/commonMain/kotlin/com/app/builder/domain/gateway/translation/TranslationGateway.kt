package com.app.builder.domain.gateway.translation

import kotlin.uuid.Uuid
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.withContext
import io.ktor.client.HttpClient
import app.cash.sqldelight.async.coroutines.awaitAsList
import com.app.builder.core.flow.Dispatcher
import com.app.builder.core.telemetry.Telemetry
import com.app.builder.data.database.asFlow
import com.app.builder.data.database.safeTransaction
import com.app.builder.data.http.HttpRequest
import com.app.builder.data.http.HttpResult
import com.app.builder.data.http.URL
import com.app.builder.data.http.get
import com.app.builder.data.resource.JsonResource
import com.app.builder.data.resource.loadResource
import com.app.builder.domain.EntityType
import com.app.builder.domain.Translation
import com.app.builder.domain.scheduler.Job
import com.app.builder.domain.scheduler.JobResult
import com.app.builder.domain.scheduler.JobResult.Success.toJobResult
import com.app.builder.domain.scheduler.Scheduler
import com.app.builder.domain.scheduler.toHeaderMap
import database.AppDatabase

/**
 * Translation repository.
 *
 * @property database The SQLite database instance for translation data.
 * @property httpClient The HTTP client used for network operations.
 * @property scheduler The job scheduler for background tasks.
 */
class TranslationGateway(
    private val database: AppDatabase,
    private val httpClient: HttpClient,
    private val scheduler: Scheduler
): TranslationRepository {

    /** Query interface for the translations table. */
    private val translationDao = database.translationQueries

    override fun observeTranslations(languageIso: String): Flow<List<Translation>> =
        translationDao.getTranslations(languageIso = languageIso)
            .asFlow { it.toTranslation() }
            .catch {
                Telemetry.error(tag = TAG, message = "Unable to observe translations", throwable = it)
                emit(value = emptyList())
            }

    override suspend fun syncTranslations() = withContext(context = Dispatcher.IO) {
        database.safeTransaction {
            if (isTranslationEmpty()) {
                val translations: List<Translation> = loadResource<Translation>(json = JsonResource.Translations)
                rewriteTranslations(translations = translations)
                Telemetry.info(tag = TAG, message = "Prepopulated translations")
            }
        }.onFailure {
            Telemetry.error(tag = TAG, message = "Unable to rewrite translations", throwable = it)
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
                    rewriteTranslations(translations = result.data)
                }.onFailure {
                    Telemetry.error(tag = TAG, message = "Unable to rewrite translations", throwable = it)
                    return@withContext it.toJobResult()
                }
                Telemetry.info(tag = TAG, message = "Successfully fetched translations")
                JobResult.Success
            }
        }
    }

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
        translations.forEach { translationDao.upsertTranslation(Translation = it.toTranslationSchema()) }
    }

    companion object {
        private const val TAG = "TranslationsGateway"
    }
}
