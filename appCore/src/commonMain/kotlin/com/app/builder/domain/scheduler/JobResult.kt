package com.app.builder.domain.scheduler

import com.app.builder.data.http.plugin.InternetDisabledException

/** Represents the final outcome of a persistent background execution unit handled by the scheduling subsystem. */
sealed interface JobResult {
    /** Indicates that there is no background execution associated with this job. */
    data object NoOp: JobResult
    /** Indicates that the job has completed its execution successfully. */
    data object Success: JobResult
    /** Indicates that the job failed to complete but is eligible to be queued again by the scheduler. */
    data object Retry: JobResult
    /**
     * Indicates that the job encountered a terminal failure and should not be retried.
     *
     * @property error The underlying exception that caused the job to fail.
     */
    data class Error(val error: Throwable): JobResult

    /**
     * Converts a [Throwable] into an appropriate [JobResult].
     * Transient network issues are mapped to [Retry] to allow the scheduler to execute the job once connectivity is restored.
     * All other exceptions are considered terminal.
     *
     * @return [Retry] if the exception is an [InternetDisabledException], otherwise an [Error].
     */
    fun Throwable.toJobResult(): JobResult =
        if (this is InternetDisabledException) Retry else Error(error = this)
}