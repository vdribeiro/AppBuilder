package com.app.builder.domain.gateway.account

import com.app.builder.domain.scheduler.Job
import com.app.builder.domain.scheduler.JobResult

/** Defines the repository for accounts. */
interface AccountRepository {

    /**
     * Attempts to fetch the account of the user in the [Job.entityUuid] from the remote server.
     *
     * @param job The job configuration blueprint.
     * @return The [JobResult].
     */
    suspend fun executeSyncAccount(job: Job): JobResult
}
