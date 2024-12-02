package com.app.builder.core.locale

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Clock
import kotlin.time.Duration
import com.app.builder.test.TestCase

class DateTimeTest: TestCase() {

    /** Verifies now returns the current instant, bounded by the system clock immediately before and after the call. */
    @Test
    fun nowReturnsCurrentInstant() = runUnitTest {
        val before = Clock.System.now()
        val instant = now()
        val after = Clock.System.now()
        assertTrue(actual = instant in before..after)
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
}
