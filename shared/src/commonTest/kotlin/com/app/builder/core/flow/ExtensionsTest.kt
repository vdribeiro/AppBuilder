package com.app.builder.core.flow

import kotlin.test.Test
import kotlin.coroutines.cancellation.CancellationException
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.cancel
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import com.app.builder.test.TestCase

class ExtensionsTest: TestCase() {

    /** Verifies the value returned by the block is wrapped as a success. */
    @Test
    fun returnsTheBlockResultAsSuccess() = runUnitTest {
        assertEquals(expected = Result.success(value = 1), actual = runSuspendCatching { 1 })
    }

    /** Verifies an exception thrown by the block is wrapped as a failure. */
    @Test
    fun returnsTheBlockExceptionAsFailure() = runUnitTest {
        val result = runSuspendCatching<Int> { error(message = "boom") }

        assertTrue(actual = result.isFailure)
        assertEquals(expected = "boom", actual = result.exceptionOrNull()?.message)
    }

    /** Verifies a cancellation thrown by the block while the calling coroutine is active is wrapped as a failure. */
    @Test
    fun returnsCancellationAsFailureWhileCallerIsActive() = runUnitTest {
        val result = runSuspendCatching<Int> { throw CancellationException("boom") }

        assertIs<CancellationException>(value = result.exceptionOrNull())
    }

    /** Verifies the cancellation of the calling coroutine is rethrown instead of being wrapped as a failure. */
    @Test
    fun rethrowsCancellationOfTheCaller() = runUnitTest {
        var result: Result<Int>? = null

        val job = launch {
            result = runSuspendCatching {
                currentCoroutineContext().cancel()
                currentCoroutineContext().ensureActive()
                1
            }
        }
        job.join()

        assertTrue(actual = job.isCancelled)
        assertNull(actual = result)
    }
}
