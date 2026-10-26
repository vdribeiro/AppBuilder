package com.app.builder.core.locale

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Clock
import kotlin.time.Duration
import kotlin.time.Instant
import com.app.builder.test.TestCase

class DateTimeTest: TestCase() {

    /** Verifies now reads whichever clock is installed rather than the system clock directly. */
    @Test
    fun nowReadsTheInstalledClock() = runUnitTest {
        val previous = DateTime.clock
        val fixed = Instant.parse(input = "2030-05-05T10:15:00Z")
        DateTime.clock = object: Clock {
            override fun now(): Instant = fixed
        }
        try {
            assertEquals(expected = fixed, actual = now())
        } finally {
            DateTime.clock = previous
        }
    }

    /** Verifies epoch returns the current epoch millisecond timestamp matching now. */
    @Test
    fun epochMatchesNow() = runUnitTest {
        val before = now().toEpochMilliseconds()
        val epoch = epoch()
        val after = now().toEpochMilliseconds()
        assertTrue(actual = epoch in before..after)
    }

    /** Verifies markNow produces a mark whose elapsedNow duration is non-negative. */
    @Test
    fun markNowMeasuresElapsedTime() = runUnitTest {
        val mark = markNow()
        assertTrue(actual = mark.elapsedNow() >= Duration.ZERO)
    }

    /** Verifies toInstant parses a valid ISO-8601 string back into the original instant. */
    @Test
    fun toInstantParsesIsoString() = runUnitTest {
        val instant = now()
        assertEquals(expected = instant, actual = instant.toString().toInstant())
    }

    /** Verifies toInstant returns null when parsing an invalid timestamp string. */
    @Test
    fun toInstantReturnsNullOnInvalidInput() = runUnitTest {
        assertNull(actual = "not a timestamp".toInstant())
    }

    /** Verifies toInstant returns null when parsing an empty string. */
    @Test
    fun toInstantReturnsNullOnEmptyInput() = runUnitTest {
        assertNull(actual = "".toInstant())
    }

    /** Verifies toInstant returns null when the string is missing its required timezone offset. */
    @Test
    fun toInstantReturnsNullWhenMissingOffset() = runUnitTest {
        assertNull(actual = "2024-01-01T00:00:00".toInstant())
    }

    /** Verifies toInstant converts epoch milliseconds into the matching instant. */
    @Test
    fun toInstantParsesEpochMilliseconds() = runUnitTest {
        assertEquals(expected = Instant.parse(input = "2030-05-05T10:15:00Z"), actual = 1_904_206_500_000L.toInstant())
        assertEquals(expected = Instant.parse(input = "1970-01-01T00:00:00Z"), actual = 0L.toInstant())
    }
}
