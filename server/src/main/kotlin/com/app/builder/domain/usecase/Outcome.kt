package com.app.builder.domain.usecase

/**
 * The outcome of a use case, telling apart why it did not succeed.
 *
 * @param T The type of the data produced on success. Writes produce [Unit].
 */
sealed interface Outcome<out T> {
    /**
     * The use case succeeded.
     *
     * @property data The data it produced.
     */
    data class Success<T>(val data: T): Outcome<T>

    /** The use case did not succeed. */
    sealed interface Failure: Outcome<Nothing>

    /** The input is inconsistent. */
    data object Invalid: Failure

    /** The credential is unknown, wrong or expired. */
    data object Rejected: Failure

    /** The entity it targets doesn't exist. */
    data object NotFound: Failure

    /** It clashes with an existing entity. */
    data object Conflict: Failure

    /** It failed on the server's side. */
    data object Error: Failure
}

/**
 * Maps a boolean to [Outcome].
 *
 * @receiver boolean value.
 * @return [Outcome.Success] when true, [Outcome.Error] otherwise.
 */
fun Boolean.toOutcome(): Outcome<Unit> = if (this) Outcome.Success(data = Unit) else Outcome.Error

/**
 * Unwraps a successful outcome, for callers that treat every failure alike.
 *
 * @receiver The outcome.
 * @return The data of a successful outcome, or `null` for any failure.
 */
fun <T> Outcome<T>.getOrNull(): T? = (this as? Outcome.Success)?.data
