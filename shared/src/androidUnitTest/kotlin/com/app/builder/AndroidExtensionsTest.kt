package com.app.builder

import kotlin.test.Test
import kotlin.test.assertEquals
import com.app.builder.test.TestCase

class AndroidExtensionsTest: TestCase() {

    /** Verifies toHex formats a known byte array as an uppercase hex string. */
    @Test
    fun toHexFormatsKnownBytes() = runUnitTest {
        val bytes = byteArrayOf(0x0A, 0x1B, 0xFF.toByte())
        assertEquals(expected = "0A1BFF", actual = bytes.toHex())
    }

    /** Verifies toHex returns an empty string for an empty byte array. */
    @Test
    fun toHexReturnsEmptyStringForEmptyArray() = runUnitTest {
        assertEquals(expected = "", actual = ByteArray(size = 0).toHex())
    }
}
