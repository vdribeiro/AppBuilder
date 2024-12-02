package com.app.builder.data.http

/**
 * Represents the outcome of a network request, encapsulating either a successful response payload or a caught exception.
 *
 * @param T The expected domain model type contained within a successful response.
 */
sealed interface HttpResult<T> {

    /**
     * Indicates a successful network operation where data was retrieved and decoded.
     *
     * @param T The expected domain model type.
     * @property data The parsed domain returned by the server.
     */
    data class Success<T>(val data: T): HttpResult<T>

    /**
     * Indicates a failed network operation resulting from connectivity issues, server anomalies, or parsing errors.
     *
     * @param T The expected domain model type.
     * @property error The caught exception detailing the specific reason for the failure.
     */
    data class Error<T>(val error: Throwable): HttpResult<T>
}
