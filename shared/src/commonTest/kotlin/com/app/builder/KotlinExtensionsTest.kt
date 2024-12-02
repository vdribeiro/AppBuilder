package com.app.builder

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import com.app.builder.test.TestCase

class KotlinExtensionsTest: TestCase() {

    private enum class Sample { PLAIN, TWO_WORDS }

    /** Verifies toEnumOrNull matches enum constants case-insensitively and returns null for no match. */
    @Test
    fun toEnumOrNull() = runUnitTest {
        assertEquals(expected = Sample.PLAIN, actual = "plain".toEnumOrNull<Sample>())
        assertEquals(expected = Sample.TWO_WORDS, actual = "TWO_WORDS".toEnumOrNull<Sample>())
        assertEquals(expected = Sample.TWO_WORDS, actual = "two words".toEnumOrNull<Sample>())
        assertEquals(expected = Sample.TWO_WORDS, actual = "TWO WORDS".toEnumOrNull<Sample>())

        assertNull(actual = "".toEnumOrNull<Sample>())
        assertNull(actual = "not a sample".toEnumOrNull<Sample>())
    }

    /** Verifies plusOrMinus adds an absent element, removes a present one, and adds into an empty collection. */
    @Test
    fun plusOrMinus() = runUnitTest {
        assertEquals(expected = listOf(1, 2, 3), actual = listOf(1, 2).plusOrMinus(element = 3))
        assertEquals(expected = listOf(1), actual = listOf(1, 2).plusOrMinus(element = 2))
        assertEquals(expected = listOf(1), actual = emptyList<Int>().plusOrMinus(element = 1))
    }
}
