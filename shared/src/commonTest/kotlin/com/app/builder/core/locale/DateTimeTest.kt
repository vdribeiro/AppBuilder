package com.app.builder.core.locale

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Clock
import kotlin.time.Duration
import kotlin.time.Instant
import com.app.builder.test.TestCase

class DateTimeTest: TestCase() {

    /** Verifies UNIX_EPOCH is the instant of the Unix epoch. */
    @Test
    fun unixEpoch() = runUnitTest {
        assertEquals(expected = Instant.parse(input = "1970-01-01T00:00:00Z"), actual = Instant.UNIX_EPOCH)
    }

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

    /** Verifies toLocalDateMillis, toLocalHour and toLocalMinute read the calendar day and time of day of the given time zone. */
    @Test
    fun localPartsFollowTheTimeZone() = runUnitTest {
        val instant = Instant.parse(input = "2026-07-09T23:30:00Z")
        val ninth = Instant.parse(input = "2026-07-09T00:00:00Z").toEpochMilliseconds()
        val tenth = Instant.parse(input = "2026-07-10T00:00:00Z").toEpochMilliseconds()

        assertEquals(expected = ninth, actual = instant.toLocalDateMillis(zoneId = "UTC"))
        assertEquals(expected = 23, actual = instant.toLocalHour(zoneId = "UTC"))
        assertEquals(expected = 30, actual = instant.toLocalMinute(zoneId = "UTC"))

        assertEquals(expected = tenth, actual = instant.toLocalDateMillis(zoneId = "UTC+01:00"))
        assertEquals(expected = 0, actual = instant.toLocalHour(zoneId = "UTC+01:00"))
        assertEquals(expected = 30, actual = instant.toLocalMinute(zoneId = "UTC+01:00"))

        assertEquals(expected = ninth, actual = instant.toLocalDateMillis(zoneId = "UTC-05:30"))
        assertEquals(expected = 18, actual = instant.toLocalHour(zoneId = "UTC-05:30"))
        assertEquals(expected = 0, actual = instant.toLocalMinute(zoneId = "UTC-05:30"))
    }

    /** Verifies toLocalDateMillis returns midnight UTC of the local day for an instant before the Unix epoch. */
    @Test
    fun toLocalDateMillisBeforeEpoch() = runUnitTest {
        val instant = Instant.parse(input = "1969-12-31T23:30:00Z")
        assertEquals(expected = -86_400_000L, actual = instant.toLocalDateMillis(zoneId = "UTC"))
        assertEquals(expected = 0L, actual = instant.toLocalDateMillis(zoneId = "UTC+01:00"))
    }

    /** Verifies withLocalDate moves to the given calendar day of the time zone and keeps the time of day. */
    @Test
    fun withLocalDateKeepsTheTimeOfDay() = runUnitTest {
        val instant = Instant.parse(input = "2026-07-09T23:30:15Z")
        val twentieth = Instant.parse(input = "2026-07-20T00:00:00Z").toEpochMilliseconds()

        assertEquals(expected = Instant.parse(input = "2026-07-20T23:30:15Z"), actual = instant.withLocalDate(dateMillis = twentieth, zoneId = "UTC"))
        assertEquals(expected = Instant.parse(input = "2026-07-19T23:30:15Z"), actual = instant.withLocalDate(dateMillis = twentieth, zoneId = "UTC+01:00"))
        assertEquals(expected = Instant.parse(input = "2026-07-20T23:30:15Z"), actual = instant.withLocalDate(dateMillis = twentieth + 3_600_000L, zoneId = "UTC"))
        assertEquals(expected = Instant.parse(input = "1969-12-31T23:30:15Z"), actual = instant.withLocalDate(dateMillis = -1L, zoneId = "UTC"))
    }

    /** Verifies withLocalTime moves to the given time of day of the time zone, keeps the calendar day and drops the seconds. */
    @Test
    fun withLocalTimeKeepsTheCalendarDay() = runUnitTest {
        val instant = Instant.parse(input = "2026-07-09T23:30:15Z")

        assertEquals(expected = Instant.parse(input = "2026-07-09T08:05:00Z"), actual = instant.withLocalTime(hour = 8, minute = 5, zoneId = "UTC"))
        assertEquals(expected = Instant.parse(input = "2026-07-10T07:05:00Z"), actual = instant.withLocalTime(hour = 8, minute = 5, zoneId = "UTC+01:00"))
    }

    /** Verifies withLocalTime throws when the hour or the minute is out of range. */
    @Test
    fun withLocalTimeThrowsWhenOutOfRange() = runUnitTest {
        assertFailsWith<IllegalArgumentException> { now().withLocalTime(hour = 24, minute = 0, zoneId = "UTC") }
        assertFailsWith<IllegalArgumentException> { now().withLocalTime(hour = 0, minute = 60, zoneId = "UTC") }
    }

    /** Verifies the local parts of an instant in the device's time zone rebuild the same instant. */
    @Test
    fun localPartsRoundTripInTheDeviceTimeZone() = runUnitTest {
        val instant = Instant.parse(input = "2026-01-15T12:34:00Z")
        val rebuilt = Instant.UNIX_EPOCH
            .withLocalDate(dateMillis = instant.toLocalDateMillis())
            .withLocalTime(hour = instant.toLocalHour(), minute = instant.toLocalMinute())
        assertEquals(expected = instant, actual = rebuilt)
    }
}
