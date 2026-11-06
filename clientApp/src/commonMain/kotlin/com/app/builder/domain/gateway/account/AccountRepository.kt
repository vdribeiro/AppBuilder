package com.app.builder.domain.gateway.account

import com.app.builder.domain.scheduler.Job
import com.app.builder.domain.scheduler.JobResult

/** Defines the repository for accounts. */
interface AccountRepository {

    /** Enqueues accounts synchronization. */
    suspend fun syncAccounts()

    /**
     * Attempts to fetch the latest accounts from the remote server.
     *
     * @param job The job configuration blueprint.
     * @return The [JobResult].
     */
    suspend fun executeSyncAccounts(job: Job): JobResult
}
