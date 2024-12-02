package com.app.builder.core.locale

import java.util.Locale
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.launch
import com.app.builder.test.TestCase

/** Desktop-specific coverage for [observeLocale] that requires mutating the real JVM default locale, which is only meaningfully controllable on this target. */
class LocaleDesktopTest: TestCase() {

    /** Verifies that [observeLocale] emits a new value once the underlying system locale actually changes. */
    @Test
    fun observeLocaleEmitsOnActualLocaleChange() = runUnitTest {
        val originalLocale = Locale.getDefault()
        try {
            Locale.setDefault(Locale.forLanguageTag("en-US"))
            val emissions = mutableListOf<String>()
            val job = backgroundScope.launch { observeLocale().collect { emissions.add(element = it) } }
            testScheduler.runCurrent()

            Locale.setDefault(Locale.forLanguageTag("pt-PT"))
            testScheduler.advanceTimeBy(delayTimeMillis = INTERVAL_MILLIS)
            testScheduler.runCurrent()

            assertEquals(expected = listOf("en-US", "pt-PT"), actual = emissions)
            job.cancel()
        } finally {
            Locale.setDefault(originalLocale)
        }
    }

    private companion object {
        /** The interval, in milliseconds, matching the desktop locale-change loop's default polling interval. */
        private const val INTERVAL_MILLIS = 3000L
    }
}
