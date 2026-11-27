package com.app.builder.core.locale

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import com.app.builder.test.TestCase

class LocalDateTimeTest: TestCase() {
    override suspend fun afterTest() {
        Language.reset()
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

    /** Verifies that [getLocalDateTime] formats with the conventions of the given language, and with those of [Language] when none is given. */
    @Test
    fun getLocalDateTimeFollowsLanguage() = runUnitTest {
        val utc = "2020-01-31T12:00:00Z"
        val english = getLocalDateTime(utc = utc, languageIso = "en")
        val german = getLocalDateTime(utc = utc, languageIso = "de")
        assertTrue(actual = english != german)

        Language.set { "de" }
        assertEquals(expected = german, actual = getLocalDateTime(utc = utc))
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

    /** Verifies that [getLocalDateTime] with only the date parts and only the time parts formats the two halves of the full string. */
    @Test
    fun getLocalDateTimeDateAndTimeParts() = runUnitTest {
        val utc = "2020-01-31T12:00:00Z"
        val date = getLocalDateTime(utc = utc, languageIso = "de", parts = DateTimeParts.DATE)
        val time = getLocalDateTime(utc = utc, languageIso = "de", parts = DateTimeParts.TIME)
        val dateTime = getLocalDateTime(utc = utc, languageIso = "de")

        assertTrue(actual = date.isNotEmpty() && time.isNotEmpty() && date != time)
        assertTrue(actual = dateTime.startsWith(prefix = date) && dateTime.endsWith(suffix = time))
        assertTrue(actual = date != getLocalDateTime(utc = utc, languageIso = "en", parts = DateTimeParts.DATE))
    }

    /** Verifies that [getLocalDateTime] leaves out each part that is not included. */
    @Test
    fun getLocalDateTimeLeavesOutExcludedParts() = runUnitTest {
        val utc = "2020-06-15T12:00:00Z"
        val date = getLocalDateTime(utc = utc, languageIso = "de", parts = DateTimeParts.DATE)
        val monthDay = getLocalDateTime(utc = utc, languageIso = "de", parts = DateTimeParts(year = false, hour = false, minute = false))
        val yearMonth = getLocalDateTime(utc = utc, languageIso = "de", parts = DateTimeParts(day = false, hour = false, minute = false))
        val year = getLocalDateTime(utc = utc, languageIso = "de", parts = DateTimeParts(month = false, day = false, hour = false, minute = false))

        assertTrue(actual = "2020" in date)
        assertTrue(actual = "1" in date && "6" in date)
        assertTrue(actual = monthDay.isNotEmpty() && "2020" !in monthDay)
        assertTrue(actual = "2020" in yearMonth && "15" !in yearMonth && "16" !in yearMonth)
        assertTrue(actual = "2020" in year && "6" !in year)
    }

    /** Verifies that [getLocalDateTime] returns an empty string when no part is included. */
    @Test
    fun getLocalDateTimeWithoutParts() = runUnitTest {
        assertEquals(expected = "", actual = getLocalDateTime(utc = "2020-06-15T12:00:00Z", parts = DateTimeParts(year = false, month = false, day = false, hour = false, minute = false)))
    }

    /** Verifies that [getLocalDateTime] returns the input unchanged when it is invalid, whichever parts are included. */
    @Test
    fun getLocalDateTimePartsInvalid() = runUnitTest {
        assertEquals(expected = "invalid", actual = getLocalDateTime(utc = "invalid", parts = DateTimeParts.DATE))
        assertEquals(expected = "invalid", actual = getLocalDateTime(utc = "invalid", parts = DateTimeParts.TIME))
    }

    /** Verifies that [getLocalDateTime] formats with the device conventions when no language is given or selected. */
    @Test
    fun getLocalDateTimeWithoutLanguage() = runUnitTest {
        assertTrue(actual = getLocalDateTime(utc = "2020-01-31T12:00:00Z", languageIso = null).isNotEmpty())
        assertTrue(actual = getLocalDateTime().isNotEmpty())
    }

    /** Verifies that [DateTimeParts] includes every part by default, and that DATE and TIME include the date parts and the time parts. */
    @Test
    fun dateTimePartsPresets() = runUnitTest {
        assertEquals(expected = DateTimeParts(year = true, month = true, day = true, hour = true, minute = true), actual = DateTimeParts())
        assertEquals(expected = DateTimeParts(year = true, month = true, day = true, hour = false, minute = false), actual = DateTimeParts.DATE)
        assertEquals(expected = DateTimeParts(year = false, month = false, day = false, hour = true, minute = true), actual = DateTimeParts.TIME)
    }
}
