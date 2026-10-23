package com.app.builder.core.locale

import kotlin.time.Clock
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Instant
import kotlinx.coroutines.test.TestCoroutineScheduler

/**
 * Clock reading the test scheduler's virtual time on top of the wall clock the test started at.
 * Code under test records and compares timestamps against the same clock the test advances, so a delay the test skips over is one the recorded timestamps reflect.
 * The wall clock anchors it because libraries outside this codebase judge those timestamps against the real clock.
 *
 * @property scheduler The scheduler whose virtual time to read.
 */
class MockClock(private val scheduler: TestCoroutineScheduler): Clock {
    /** The wall clock reading this clock counts virtual time from. */
    private val start: Instant = Clock.System.now()

    override fun now(): Instant = start + scheduler.currentTime.milliseconds
}