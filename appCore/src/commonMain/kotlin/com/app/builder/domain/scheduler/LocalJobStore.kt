package com.app.builder.domain.scheduler

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map

/** Implementation of [JobStore] that stores jobs in memory. */
class LocalJobStore: JobStore {

    /** Backing state for [jobs]. */
    private val _jobs = MutableStateFlow<List<Job>>(value = emptyList())
    /** The current list of jobs held by this in-memory store. */
    val jobs = _jobs.asStateFlow()

    override fun observePendingJobs(): Flow<List<Job>> = _jobs.map { jobs -> jobs.filter { it.state == Job.State.PENDING } }

    override suspend fun upsertJob(job: Job): Boolean {
        _jobs.value = _jobs.value.filterNot { it.uuid == job.uuid } + job
        return true
    }

    override suspend fun cancelPendingJobs(job: Job): Boolean {
        _jobs.value = _jobs.value.map {
            if (it.userUuid == job.userUuid && it.entityUuid == job.entityUuid && it.entityType == job.entityType && it.type == job.type && it.state == Job.State.PENDING) {
                it.copy(state = Job.State.CANCELED)
            } else it
        }
        return true
    }

    override suspend fun getDuplicateJobsCount(job: Job): Long = _jobs.value.count {
        it.userUuid == job.userUuid && it.entityUuid == job.entityUuid && it.entityType == job.entityType && it.type == job.type && it.state == Job.State.PENDING
    }.toLong()

    override suspend fun resetZombieJobs(): Long {
        val runningJobs = _jobs.value.filter { it.state == Job.State.RUNNING }
        _jobs.value = _jobs.value.map { if (it.state == Job.State.RUNNING) it.copy(state = Job.State.PENDING) else it }
        return runningJobs.size.toLong()
    }

    override suspend fun updateJobStateAndAttempt(job: Job): Boolean {
        _jobs.value = _jobs.value.map { if (it.uuid == job.uuid) it.copy(state = job.state, attempt = job.attempt) else it }
        return true
    }
}