package com.app.builder.domain.gateway.translation

import com.app.builder.domain.scheduler.Job
import com.app.builder.domain.scheduler.JobResult
import com.app.builder.domain.translation.TranslationStore

/** Defines the core contract for pulling and reconciling system localization assets. */
interface TranslationRepository: TranslationStore {

    /**
     * Attempts to fetch the latest translations from the remote server.
     * If successful, it stores the fetched translations that are not older than the local ones and deletes the local translations the server no longer has.
     *
     * @param job The job configuration blueprint.
     * @return The [JobResult].
     */
    suspend fun executeSyncTranslations(job: Job): JobResult

    /**
     * Uploads the translation of the job payload and stores the translation the server answers with, unless the local one is newer.
     *
     * @param job The job carrying the encoded translation.
     * @return The [JobResult].
     */
    suspend fun executeUpdateTranslation(job: Job): JobResult
}