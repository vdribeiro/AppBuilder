package com.app.builder.domain.scheduler

import kotlinx.io.IOException
import io.ktor.http.HttpStatusCode
import com.app.builder.data.http.plugin.InternetDisabledException
import com.app.builder.data.http.plugin.UnsuccessfulStatusException

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
     * Transient failures are mapped to [Retry] so the scheduler can retry the job again.
     * All other exceptions are considered terminal.
     *
     * @return [Retry] if the exception is transient, otherwise an [Error].
     */
    fun Throwable.toJobResult(): JobResult = when (this) {
        is InternetDisabledException,
        is IOException -> Retry

        is UnsuccessfulStatusException if status.isRetryable -> Retry
        else -> Error(error = this)
    }

    /** Determines whether an HTTP status code indicates a transient error that should be retried. */
    private val HttpStatusCode.isRetryable: Boolean
        get() = when (this) {
            HttpStatusCode.RequestTimeout,
            HttpStatusCode.TooManyRequests -> true

            else -> value in 500..599
        }
}