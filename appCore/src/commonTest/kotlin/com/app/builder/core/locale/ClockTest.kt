package com.app.builder.core.locale

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.launch
import com.app.builder.test.TestCase

class ClockTest: TestCase() {

    /**
     * Verifies that [observeClockChanges] does not emit while the system and monotonic clocks stay in sync, exercising the loop's divergence check on every tick without a real clock change.
     * A genuine clock-change emission can't be exercised hermetically here, since it depends on the real OS clock jumping relative to the monotonic clock.
     */
    @Test
    fun observeClockChangesDoesNotEmitUnderNormalConditions() = runUnitTest {
        var emissions = 0
        val job = backgroundScope.launch { observeClockChanges().collect { emissions++ } }

        testScheduler.runCurrent()
        testScheduler.advanceTimeBy(delayTimeMillis = INTERVAL_MILLIS)
        testScheduler.runCurrent()
        testScheduler.advanceTimeBy(delayTimeMillis = INTERVAL_MILLIS)
        testScheduler.runCurrent()

        assertEquals(expected = 0, actual = emissions)
        job.cancel()
    }

    private companion object {
        /** The interval, in milliseconds, matching the desktop clock-change loop's default polling interval. */
        private const val INTERVAL_MILLIS = 3000L
    }
}
