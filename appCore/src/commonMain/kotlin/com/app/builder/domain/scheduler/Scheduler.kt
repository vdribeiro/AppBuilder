package com.app.builder.domain.scheduler

/**
 * Defines the core capabilities for staging and tracking asynchronous background work.
 * The [Scheduler] orchestrates dispatch logic by serving as the primary bridge to append, modify, or verify job queues destined for persistent execution.
 */
interface Scheduler {

    /** Stops the scheduler loop, suspending until it has fully canceled. */
    suspend fun stop()

    /**
     * Initiates a reactive, persistent stream to observe pending jobs.
     * Combines the database emission with the internet availability stream.
     * If offline, it maps to an empty list to pause execution.
     * When online, pending jobs are routed to independent chains, respecting the in-memory execution lock to prevent duplicate dispatches.
     * If the service is already running, this is a no-op.
     */
    fun start()

    /**
     * Enqueues a new background execution payload into local storage.
     * Evaluates the declared [Job.ConflictPolicy] before writing to disk.
     *
     * @param job The job configuration blueprint.
     */
    suspend fun queue(job: Job)
}