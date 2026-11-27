package com.app.builder.core.locale

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import com.app.builder.test.TestCase

class LanguageTest: TestCase() {

    /** Verifies that no language is selected by default, so the app language is the device language. */
    @Test
    fun followsDeviceLanguageByDefault() = runUnitTest {
        assertNull(actual = Language.value)
        assertEquals(expected = "pt", actual = observeLanguage(locale = flowOf(value = "pt")).first())
    }

    /** Verifies that a selected language replaces the device language as the app language, until none is selected again. */
    @Test
    fun selectedLanguageReplacesDeviceLanguage() = runUnitTest {
        Language.set { "en" }
        assertEquals(expected = "en", actual = observeLanguage(locale = flowOf(value = "pt")).first())

        Language.reset()
        assertEquals(expected = "pt", actual = observeLanguage(locale = flowOf(value = "pt")).first())
    }
}
