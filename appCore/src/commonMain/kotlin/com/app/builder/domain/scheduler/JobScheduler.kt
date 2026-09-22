package com.app.builder.domain.scheduler

import kotlin.uuid.Uuid
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import com.app.builder.core.config.ClientConfigs
import com.app.builder.core.config.ClientFlags
import com.app.builder.core.flow.Dispatcher
import com.app.builder.core.telemetry.Telemetry
import com.app.builder.data.http.getInternetAvailabilityFlow
import com.app.builder.data.http.plugin.InternetDisabledException
import com.app.builder.domain.EntityType
import com.app.builder.domain.clock.ClockService
import kotlinx.coroutines.Job as CoroutinesJob

/**
 * A persistent, fault-tolerant background engine that observes, enqueues, and dispatches scheduled [Job] workflows.
 *
 * ### Architecture Overview
 * This scheduler acts as the core coordinator for the application's Offline-First architecture.
 * Instead of keeping jobs in volatile memory, every job is immediately persisted in the database.
 * A reactive stream observes this database and pairs it with the device's live network status and clock state.
 * If the device goes offline, or its clock drifts from the server beyond tolerance, the stream pauses.
 * It then resumes automatically once connectivity and clock trust are restored.
 *
 * ### Concurrency & Ordering Rules (The "Chain" System)
 * To prevent database race conditions and ensure data integrity, jobs are grouped into logical `Chains`.
 * A Chain is defined by its User, Entity Type, and optionally an Entity UUID.
 * * 1. **Targeted Jobs (Mutations - POST, DELETE):**
 * Jobs modifying a specific entity, e.g. updating Domain A, are strictly sequential.
 * If 3 updates are queued for Domain A, they execute one by one in chronological order.
 * However, updates for Domain A and Domain B will execute in parallel across different chains.
 * * 2. **Global Jobs (Full Syncs - GET):**
 * Jobs fetching full collections require a clean slate.
 * A Global Job will **suspend execution** if there are *any* pending or actively running Targeted Jobs for that specific Entity Type.
 * This guarantees that local unsynced mutations are pushed to the server *before* the app pulls the latest global state, preventing data overwrites.
 * The guard is symmetric: while a Global Job is running for an Entity Type, Targeted Jobs for that same type are held back until it completes,
 * so a full sync never interleaves with mutations against the table it is reading.
 *
 * ### Lifecycle & Crash Recovery
 * - **State Machine:** Jobs transition from `PENDING` -> `RUNNING` -> `COMPLETE` / `FAILED`.
 * - **Defensive Failures:** If a catastrophic runtime exception occurs during execution, the job safely falls back to a `FAILED` state to prevent infinite loops.
 * - **Zombie Cleanup:** If the OS kills the app while a job is `RUNNING`, that job would otherwise remain stuck forever. To fix this, on every startup all orphaned `RUNNING` jobs are reset back to `PENDING` before the scheduler loop begins.
 *
 * @property jobStore The persistence coordinator used to observe and manipulate queued jobs.
 * @property jobFactory The factory mapping raw metadata rows to executable domain layer workloads.
 * @property clockService The service managing the device's clock.
 */
class JobScheduler(
    private val jobStore: JobStore,
    private val jobFactory: JobFactory,
    private val clockService: ClockService
): Scheduler {

    /** An isolated scope managing background worker routines. */
    private val scope = CoroutineScope(context = SupervisorJob())

    /** Tracks the current scheduler loop. */
    private var job: CoroutinesJob? = null

    /** Guards concurrent start()/stop() transitions. */
    private val mutex = Mutex()

    /** Concurrency lock to prevent race conditions during reactive database flow emissions. */
    private val activeChainsMutex = Mutex()

    /** In-memory registry of currently executing job chains across different loop emissions. */
    private val activeChains = mutableSetOf<Chain>()

    /**
     * Identifies a sequential execution queue for background jobs, preventing overlapping operations on identical targets.
     *
     * @property userUuid The user associated with the jobs.
     * @property entityType The domain type of the jobs.
     * @property entityUuid The specific entity identifier, or null for global operations.
     */
    private data class Chain(
        val userUuid: Uuid,
        val entityType: EntityType,
        val entityUuid: Uuid?
    )

    override suspend fun stop() = withContext(context = Dispatcher.IO) {
        mutex.withLock {
            job?.cancelAndJoin()
            job = null
        }
    }

    override fun start() {
        if (!ClientFlags.flags.scheduler) return
        if (!mutex.tryLock()) return

        try {
            startLoop()
        } finally {
            mutex.unlock()
        }
    }

    override suspend fun queue(job: Job) = withContext(context = Dispatcher.IO) {
        if (!ClientFlags.flags.scheduler) {
            // Execute jobs when scheduler is disabled
            scope.launch(context = Dispatcher.IO) { jobFactory.resolve(job = job).invoke() }
            return@withContext
        }

        runCatching {
            when (job.conflictPolicy) {
                Job.ConflictPolicy.REPLACE -> {
                    cancelPendingJobs(job = job)
                    upsertJob(job = job)
                }

                Job.ConflictPolicy.APPEND -> upsertJob(job = job)

                Job.ConflictPolicy.IGNORE -> {
                    val count = getDuplicateJobsCount(job = job)
                    if (count == 0L) upsertJob(job = job)
                }
            }
        }.onFailure {
            Telemetry.error(tag = TAG, message = "Unable to queue job", throwable = it)
        }
    }

    /**
     * Launches the reactive scheduler loop.
     * If the job is already running, this is a no-op.
     */
    private fun startLoop() {
        if (job?.isActive != true) job = scope.launch(context = Dispatcher.IO) {
            resetZombieJobs()
            combine(
                flow = observePendingJobs(),
                flow2 = getInternetAvailabilityFlow(),
                flow3 = clockService.trusted
            ) { jobs, online, trusted -> if (online && trusted) jobs else emptyList() }.collect { jobs ->
                if (jobs.isEmpty()) return@collect

                activeChainsMutex.withLock {
                    val userEntityGroups = jobs.groupBy { Pair(first = it.userUuid, second = it.entityType) }

                    for ((groupKey, groupJobs) in userEntityGroups) {
                        val (userUuid, entityType) = groupKey
                        val targetedJobs = groupJobs.filter { it.entityUuid != null }
                        val globalJobs = groupJobs.filter { it.entityUuid == null }
                        val globalChain = Chain(userUuid = userUuid, entityType = entityType, entityUuid = null)
                        val isGlobalActive = globalChain in activeChains

                        // Process Targeted Jobs (POST, DELETE)
                        // Do not mutate if a global full-sync is actively reading/writing to the same entity table
                        if (!isGlobalActive) {
                            val targetedChains = targetedJobs.groupBy { Chain(userUuid = userUuid, entityType = entityType, entityUuid = it.entityUuid) }
                            for ((chain, chainJobs) in targetedChains) {
                                if (chain !in activeChains) {
                                    val oldestJob = chainJobs.minByOrNull { it.utc } ?: continue
                                    activeChains.add(element = chain)
                                    executeJobAsync(loopScope = this@launch, job = oldestJob, chain = chain)
                                }
                            }
                        }

                        // Process Global Jobs (GET)
                        // Ensure no pending OR running targeted mutations exist for this entity type
                        val hasPendingTargeted = targetedJobs.isNotEmpty()
                        val hasActiveTargeted = activeChains.any { it.userUuid == userUuid && it.entityType == entityType && it.entityUuid != null }

                        if (!hasPendingTargeted && !hasActiveTargeted && globalJobs.isNotEmpty()) {
                            if (globalChain !in activeChains) {
                                val oldestGlobalJob = globalJobs.minByOrNull { it.utc } ?: continue
                                activeChains.add(element = globalChain)
                                executeJobAsync(loopScope = this@launch, job = oldestGlobalJob, chain = globalChain)
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * Claims a job, executes it asynchronously, and manages terminal state transitions.
     *
     * @param loopScope The scope backing the tracked [job].
     * @param job The background job to execute.
     * @param chain The logical grouping identifier currently holding the lock.
     */
    private fun executeJobAsync(loopScope: CoroutineScope, job: Job, chain: Chain) {
        loopScope.launch(context = Dispatcher.IO) {
            val runningJob = job.copy(state = Job.State.RUNNING)
            // Defensive default: Assume failure. If a severe crash occurs, we fail safely.
            var jobToSave: Job = runningJob.copy(state = Job.State.FAILED)
            try {
                updateJobStateAndAttempt(job = runningJob)
                val result = jobFactory.resolve(job = runningJob).invoke()
                jobToSave = when (result) {
                    is JobResult.Success,
                    is JobResult.NoOp -> runningJob.copy(state = Job.State.COMPLETE)

                    is JobResult.Retry -> applyRetry(job = runningJob)

                    is JobResult.Error -> {
                        Telemetry.error(tag = TAG, message = "Job failed: $runningJob", throwable = result.error)
                        runningJob.copy(state = Job.State.FAILED)
                    }
                }
            } catch (throwable: Throwable) {
                Telemetry.error(tag = TAG, message = "Unhandled error during job execution", throwable = throwable)
                jobToSave = if (throwable is InternetDisabledException) applyRetry(job = runningJob) else runningJob.copy(state = Job.State.FAILED)
            } finally {
                // Non-cancellable: this must persist the terminal state and release the chain even if the loop was canceled mid-execution
                withContext(context = NonCancellable) {
                    // Release the lock before writing the final terminal state to prevent deadlocks
                    activeChainsMutex.withLock { activeChains.remove(element = chain) }
                    updateJobStateAndAttempt(job = jobToSave)
                }
            }
        }
    }

    /**
     * Determines the next state for a job that returned [JobResult.Retry] or encountered a network error.
     * Increments the attempt counter and transitions to `FAILED` once the attempts reach [ClientConfigs.schedulerMaxAttempts], otherwise back to `PENDING`.
     *
     * @param job The job to process.
     * @return A mutated copy of the job reflecting the new state and attempt count.
     */
    private fun applyRetry(job: Job): Job {
        val nextAttempt = job.attempt + 1
        return if (nextAttempt >= ClientConfigs.configs.schedulerMaxAttempts) {
            job.copy(state = Job.State.FAILED, attempt = nextAttempt)
        } else job.copy(state = Job.State.PENDING, attempt = nextAttempt)
    }

    /**
     * Observes pending jobs in the store.
     *
     * @return A flow of pending jobs.
     */
    private fun observePendingJobs(): Flow<List<Job>> =
        jobStore.observePendingJobs()
            .catch {
                Telemetry.error(tag = TAG, message = "Unable to observe pending jobs", throwable = it)
                emit(value = emptyList())
            }

    /**
     * Inserts or updates a job in the local store.
     *
     * @param job The [Job] domain model containing the details to save.
     * @return `true` if the job was successfully upserted; `false` if an error occurred or no rows were affected.
     */
    private suspend fun upsertJob(job: Job): Boolean = runCatching {
        jobStore.upsertJob(job = job)
    }.onFailure {
        Telemetry.error(tag = TAG, message = "Unable to upsert job", throwable = it)
    }.getOrDefault(defaultValue = false)

    /**
     * Cancel pending jobs of the same blueprint.
     *
     * @param job The job configuration blueprint.
     * @return `true` if successful, `false` if an error occurred or no rows were affected.
     */
    private suspend fun cancelPendingJobs(job: Job): Boolean = runCatching {
        jobStore.cancelPendingJobs(job = job)
    }.onFailure {
        Telemetry.error(tag = TAG, message = "Unable to cancel pending jobs", throwable = it)
    }.getOrDefault(defaultValue = false)

    /**
     * Counts the number of duplicate jobs in the store.
     *
     * @param job The job configuration blueprint.
     * @return The count of duplicate jobs.
     */
    private suspend fun getDuplicateJobsCount(job: Job): Long = runCatching {
        jobStore.getDuplicateJobsCount(job = job)
    }.onFailure {
        Telemetry.error(tag = TAG, message = "Unable to get job count", throwable = it)
    }.getOrDefault(defaultValue = 0L)

    /**
     * Set [Job.State.RUNNING] jobs back to [Job.State.PENDING].
     *
     * @return The number of rows updated.
     */
    private suspend fun resetZombieJobs() = runCatching {
        jobStore.resetZombieJobs()
    }.onFailure {
        Telemetry.error(tag = TAG, message = "Unable to reset zombie jobs", throwable = it)
    }.getOrDefault(defaultValue = 0L)

    /**
     * Updates a job's state and attempt count in the local store.
     *
     * @param job The job to update.
     * @return `true` if the job was successfully updated; `false` if an error occurred or no rows were affected.
     */
    private suspend fun updateJobStateAndAttempt(job: Job): Boolean = runCatching {
        jobStore.updateJobStateAndAttempt(job = job)
    }.onFailure {
        Telemetry.error(tag = TAG, message = "Unable to update job", throwable = it)
    }.getOrDefault(defaultValue = false)

    companion object {
        private const val TAG = "JobScheduler"
    }
}