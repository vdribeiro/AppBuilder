package com.app.builder.core.locale

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.datetime.UtcOffset
import kotlinx.datetime.asTimeZone
import com.app.builder.test.TestCase

class TimeZoneTest: TestCase() {

    /** A time zone two hours ahead of UTC. */
    private val timeZone = UtcOffset(hours = 2).asTimeZone()

    /** Verifies that a UTC timestamp converts into the date and time of day of the time zone, moving to the next day past its midnight and dropping the seconds. */
    @Test
    fun utcToLocalUsesTheTimeZone() = runUnitTest {
        assertEquals(expected = "2026-11-27T01:15", actual = utcToLocal(utc = "2026-11-26T23:15:42Z", timeZone = timeZone))
        assertEquals(expected = "2026-11-27T02:00", actual = utcToLocal(utc = "2026-11-27T00:00:00Z", timeZone = timeZone))
    }

    /** Verifies that a local date-time converts into the UTC timestamp at the start of its minute. */
    @Test
    fun localToUtcUsesTheTimeZone() = runUnitTest {
        assertEquals(expected = "2026-11-26T23:15:00Z", actual = localToUtc(local = "2026-11-27T01:15", timeZone = timeZone))
    }

    /** Verifies that an invalid value is returned as given. */
    @Test
    fun invalidValuesAreReturnedAsGiven() = runUnitTest {
        assertEquals(expected = "nope", actual = utcToLocal(utc = "nope", timeZone = timeZone))
        assertEquals(expected = "nope", actual = localToUtc(local = "nope", timeZone = timeZone))
    }

    /** Verifies that a UTC timestamp at the start of a minute survives a round trip through the device's time zone. */
    @Test
    fun roundTripInTheDeviceTimeZone() = runUnitTest {
        assertEquals(expected = "2026-06-01T09:30:00Z", actual = localToUtc(local = utcToLocal(utc = "2026-06-01T09:30:00Z")))
    }
}
