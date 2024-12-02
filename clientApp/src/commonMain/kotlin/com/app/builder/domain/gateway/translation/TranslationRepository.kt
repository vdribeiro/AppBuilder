package com.app.builder.domain.gateway.translation

import com.app.builder.domain.scheduler.Job
import com.app.builder.domain.scheduler.JobResult
import com.app.builder.domain.translation.TranslationStore

/** Defines the core contract for pulling and reconciling system localization assets. */
interface TranslationRepository: TranslationStore {

    /**
     * Attempts to fetch the latest translations from the remote server.
     * If successful, it automatically overwrites the local database with the new data.
     *
     * @param job The job configuration blueprint.
     * @return The [JobResult].
     */
    suspend fun executeSyncTranslations(job: Job): JobResult
}