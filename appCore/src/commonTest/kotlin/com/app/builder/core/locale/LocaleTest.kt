package com.app.builder.core.locale

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import com.app.builder.test.TestCase

class LocaleTest: TestCase() {
    /** Verifies that [observeLocale] emits the current device language. */
    @Test
    fun observeLocal() = runUnitTest {
        assertEquals(expected = getLanguage(), actual = observeLocale().first())
    }

    /** Verifies that [observeLocale] keeps polling on every loop tick but only re-emits when the language actually changes, per its `distinctUntilChanged` guard. A genuine locale change is exercised separately in the desktop-specific test, since faking it here would require a JVM-only API unavailable to this shared source set. */
    @Test
    fun observeLocaleDoesNotEmitDuplicatesOnSubsequentTicks() = runUnitTest {
        val emissions = mutableListOf<String>()
        val job = backgroundScope.launch { observeLocale().collect { emissions.add(element = it) } }

        testScheduler.runCurrent()
        testScheduler.advanceTimeBy(delayTimeMillis = INTERVAL_MILLIS)
        testScheduler.runCurrent()
        testScheduler.advanceTimeBy(delayTimeMillis = INTERVAL_MILLIS)
        testScheduler.runCurrent()

        assertEquals(expected = listOf(getLanguage()), actual = emissions)
        job.cancel()
    }

    /** Verifies that [getLocalDateTime] formats a valid UTC timestamp. */
    @Test
    fun getLocalDateTimeValid() = runUnitTest {
        assertTrue(actual = getLocalDateTime(utc = "2020-01-01T12:00:00Z").isNotEmpty())
        assertTrue(actual = getLocalDateTime(utc = "2020-01-01T12:00:00.500Z").isNotEmpty())
    }

    /** Verifies that [getLocalDateTime] formats a valid UTC timestamp carrying a non-Z numeric offset. */
    @Test
    fun getLocalDateTimeValidWithNonZOffset() = runUnitTest {
        assertTrue(actual = getLocalDateTime(utc = "2020-01-01T12:00:00+02:00").isNotEmpty())
    }

    /** Verifies that [getLocalDateTime] returns the input unchanged for an empty string. */
    @Test
    fun getLocalDateTimeEmpty() = runUnitTest {
        assertEquals(expected = "", actual = getLocalDateTime(utc = ""))
    }

    /** Verifies that [getLocalDateTime] returns the input unchanged for an invalid UTC timestamp. */
    @Test
    fun getLocalDateTimeInvalid() = runUnitTest {
        val invalid = "invalidUtc"
        assertEquals(expected = invalid, actual = getLocalDateTime(utc = invalid))
    }

    private companion object {
        /** The interval, in milliseconds, matching the desktop locale-change loop's default polling interval. */
        private const val INTERVAL_MILLIS = 3000L
    }
}
