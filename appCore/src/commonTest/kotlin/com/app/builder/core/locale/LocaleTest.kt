package com.app.builder.core.locale

import kotlin.test.Test
import kotlin.test.assertEquals
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

    private companion object {
        /** The time, in milliseconds, advanced between checks; long enough to cover any platform that polls. */
        private const val INTERVAL_MILLIS = 3000L
    }
}
