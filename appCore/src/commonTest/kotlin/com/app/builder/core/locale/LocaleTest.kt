package com.app.builder.core.locale

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import com.app.builder.test.TestCase

class LocaleTest: TestCase() {
    /** Verifies that [observeDeviceLanguage] emits the current device language. */
    @Test
    fun observeDeviceLanguageEmitsCurrentLanguage() = runUnitTest {
        assertEquals(expected = getDeviceLanguage(), actual = observeDeviceLanguage().first())
    }

    /** Verifies that [observeDeviceLanguage] emits the current language once and stays silent while it does not change, even as time passes. */
    @Test
    fun observeDeviceLanguageDoesNotEmitDuplicatesOnSubsequentTicks() = runUnitTest {
        val emissions = mutableListOf<String>()
        val job = backgroundScope.launch { observeDeviceLanguage().collect { emissions.add(element = it) } }

        testScheduler.runCurrent()
        testScheduler.advanceTimeBy(delayTimeMillis = INTERVAL_MILLIS)
        testScheduler.runCurrent()
        testScheduler.advanceTimeBy(delayTimeMillis = INTERVAL_MILLIS)
        testScheduler.runCurrent()

        assertEquals(expected = listOf(getDeviceLanguage()), actual = emissions)
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

    /** Verifies that [getLocalDateTime] formats with the conventions of the given language, and with those of [SelectedLanguage] when none is given. */
    @Test
    fun getLocalDateTimeFollowsLanguage() = runUnitTest {
        val utc = "2020-01-31T12:00:00Z"
        val english = getLocalDateTime(utc = utc, languageIso = "en")
        val german = getLocalDateTime(utc = utc, languageIso = "de")
        assertTrue(actual = english != german)

        SelectedLanguage.set { "de" }
        try {
            assertEquals(expected = german, actual = getLocalDateTime(utc = utc))
        } finally {
            SelectedLanguage.reset()
        }
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

    /** Verifies that [getLocalDate] and [getLocalTime] format the two parts [getLocalDateTime] shows, with the conventions of the given language. */
    @Test
    fun getLocalDateAndTimeAreThePartsOfTheDateTime() = runUnitTest {
        val utc = "2020-01-31T12:00:00Z"
        val date = getLocalDate(utc = utc, languageIso = "de")
        val time = getLocalTime(utc = utc, languageIso = "de")
        val dateTime = getLocalDateTime(utc = utc, languageIso = "de")

        assertTrue(actual = date.isNotEmpty() && time.isNotEmpty() && date != time)
        assertTrue(actual = dateTime.startsWith(prefix = date) && dateTime.endsWith(suffix = time))
        assertTrue(actual = date != getLocalDate(utc = utc, languageIso = "en"))
    }

    /** Verifies that [getLocalDate] and [getLocalTime] return the input unchanged when it is invalid. */
    @Test
    fun getLocalDateAndTimeInvalid() = runUnitTest {
        assertEquals(expected = "invalid", actual = getLocalDate(utc = "invalid"))
        assertEquals(expected = "invalid", actual = getLocalTime(utc = "invalid"))
    }

    private companion object {
        /** The time, in milliseconds, advanced between checks; long enough to cover any platform that polls. */
        private const val INTERVAL_MILLIS = 3000L
    }
}
