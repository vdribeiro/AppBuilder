package com.app.builder.core.platform

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.launch
import com.app.builder.test.TestCase

class LoopTest: TestCase() {

    /** Verifies loop runs immediately, repeats on every interval, and stops once its job is canceled. */
    @Test
    fun loopRepeatsOnEveryIntervalAndStopsOnCancellation() = runUnitTest {
        var iterations = 0

        val job = backgroundScope.launch { loop(timeMillis = INTERVAL) { iterations++ } }

        testScheduler.runCurrent()
        assertEquals(expected = 1, actual = iterations)

        testScheduler.advanceTimeBy(delayTimeMillis = INTERVAL)
        testScheduler.runCurrent()
        assertEquals(expected = 2, actual = iterations)

        testScheduler.advanceTimeBy(delayTimeMillis = INTERVAL * 2)
        testScheduler.runCurrent()
        assertEquals(expected = 4, actual = iterations)

        job.cancel()

        testScheduler.advanceTimeBy(delayTimeMillis = INTERVAL * 5)
        testScheduler.runCurrent()

        assertEquals(expected = 4, actual = iterations)
    }

    /** Verifies loop falls back to its default 3000ms interval when none is provided. */
    @Test
    fun loopUsesDefaultInterval() = runUnitTest {
        var iterations = 0

        val job = backgroundScope.launch { loop { iterations++ } }

        testScheduler.runCurrent()
        assertEquals(expected = 1, actual = iterations)

        testScheduler.advanceTimeBy(delayTimeMillis = DEFAULT_INTERVAL)
        testScheduler.runCurrent()
        assertEquals(expected = 2, actual = iterations)

        job.cancel()
    }

    private companion object {
        /** The default interval used by loop when no timeMillis argument is supplied. */
        const val DEFAULT_INTERVAL = 3000L
        /** Interval in milliseconds used to drive the loop under test. */
        const val INTERVAL = 100L
    }
}
