package com.app.builder.core.platform

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import com.app.builder.test.TestCase

class PlatformTest: TestCase() {

    /** Verifies fromString resolves every known OS name, case-insensitively. */
    @Test
    fun osFromStringResolvesKnownNames() = runUnitTest {
        assertEquals(expected = OS.Android, actual = OS.fromString(value = "android"))
        assertEquals(expected = OS.Android, actual = OS.fromString(value = "ANDROID"))
        assertEquals(expected = OS.Ios, actual = OS.fromString(value = "ios"))
        assertEquals(expected = OS.Ios, actual = OS.fromString(value = "IOS"))
        assertEquals(expected = OS.Windows, actual = OS.fromString(value = "windows"))
        assertEquals(expected = OS.Windows, actual = OS.fromString(value = "Windows"))
        assertEquals(expected = OS.Mac, actual = OS.fromString(value = "mac"))
        assertEquals(expected = OS.Mac, actual = OS.fromString(value = "MAC"))
        assertEquals(expected = OS.Linux, actual = OS.fromString(value = "linux"))
        assertEquals(expected = OS.Linux, actual = OS.fromString(value = "LINUX"))
        assertEquals(expected = OS.Web, actual = OS.fromString(value = "web"))
        assertEquals(expected = OS.Web, actual = OS.fromString(value = "WEB"))
    }

    /** Verifies fromString falls back to Unknown for an unrecognized or empty OS name. */
    @Test
    fun osFromStringFallsBackToUnknown() = runUnitTest {
        assertEquals(expected = OS.Unknown, actual = OS.fromString(value = "not-a-real-os"))
        assertEquals(expected = OS.Unknown, actual = OS.fromString(value = ""))
    }

    /** Verifies the runtime platform metadata is populated with a known OS and non-blank fields. */
    @Test
    fun platformMetadataIsNonBlank() = runUnitTest {
        assertTrue(actual = platform.os != OS.Unknown)
        assertTrue(actual = platform.osVersion.isNotBlank())
        assertTrue(actual = platform.brand.isNotBlank())
        assertTrue(actual = platform.model.isNotBlank())
    }
}
