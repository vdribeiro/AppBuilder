package com.app.builder.core.locale

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import com.app.builder.test.TestCase

class LanguageTest: TestCase() {
    
    override suspend fun afterTest() {
        Language.reset()
    }

    /** Verifies that no language is selected until one is set, and none again once reset. */
    @Test
    fun holdsTheSelectedLanguageUntilReset() = runUnitTest {
        assertNull(actual = Language.value)

        Language.set { "pt" }
        assertEquals(expected = "pt", actual = Language.value)

        Language.reset()
        assertNull(actual = Language.value)
    }
}
