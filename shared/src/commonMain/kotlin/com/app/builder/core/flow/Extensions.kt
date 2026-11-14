package com.app.builder.core.flow

import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive

/**
 * Calls the given [block] and wraps its outcome in a [Result], rethrowing the cancellation of the calling coroutine.
 * A [CancellationException] thrown by [block] while the calling coroutine is still active is wrapped as a failure.
 *
 * @param T The type returned by [block].
 * @param block The block to call.
 * @return A [Result] holding the value returned by [block], or the exception it threw.
 * @throws CancellationException If the calling coroutine is canceled.
 */
suspend inline fun <T> runSuspendCatching(block: () -> T): Result<T> =
    runCatching(block = block).onFailure { if (it is CancellationException) currentCoroutineContext().ensureActive() }
