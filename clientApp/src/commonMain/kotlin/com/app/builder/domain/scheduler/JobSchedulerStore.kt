package com.app.builder.domain.scheduler

import kotlinx.coroutines.flow.Flow
import app.cash.sqldelight.async.coroutines.awaitAsOneOrNull
import com.app.builder.data.database.asFlow
import database.AppDatabase

/**
 * [JobStore] implementation backed by the local database.
 *
 * @param database The SQLite database instance for job persistence.
 */
class JobSchedulerStore(database: AppDatabase): JobStore {

    /** The generated queries used to read and write job rows in the local database. */
    private val jobDao = database.jobQueries

    override fun observePendingJobs(): Flow<List<Job>> =
        jobDao.getJobsByState(states = listOf(Job.State.PENDING)).asFlow { it.toJob() }

    override suspend fun upsertJob(job: Job): Boolean =
        jobDao.upsertJob(Job = job.toJobSchema()) > 0

    override suspend fun cancelPendingJobs(job: Job): Boolean =
        jobDao.updateJobs(
            userUuid = job.userUuid,
            entityUuid = job.entityUuid,
            entityType = job.entityType,
            type = job.type,
            oldState = Job.State.PENDING,
            newState = Job.State.CANCELED,
        ) > 0

    override suspend fun getDuplicateJobsCount(job: Job): Long =
        jobDao.getDuplicateJobsCount(
            userUuid = job.userUuid,
            entityUuid = job.entityUuid,
            entityType = job.entityType,
            type = job.type,
            state = Job.State.PENDING,
        ).awaitAsOneOrNull() ?: 0L

    override suspend fun resetZombieJobs(): Long =
        jobDao.resetJobsState(oldState = Job.State.RUNNING, newState = Job.State.PENDING)

    override suspend fun updateJobStateAndAttempt(job: Job): Boolean =
        jobDao.updateJobStateAndAttempt(uuid = job.uuid, state = job.state, attempt = job.attempt) > 0
}