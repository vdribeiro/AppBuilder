package com.app.builder.core.platform

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import com.app.builder.test.TestCase

class PlatformTest: TestCase() {

    /** Verifies that the current platform metadata is resolved from the host JVM's system properties, rather than left with unresolved fallback values. */
    @Test
    fun platform() = runServerTest {
        assertNotEquals(illegal = OS.Unknown, actual = platform.os)
        assertFalse(actual = platform.osVersion.isBlank())
        assertNotEquals(illegal = "unknown", actual = platform.osVersion)
        assertFalse(actual = platform.brand.isBlank())
        assertNotEquals(illegal = "unknown", actual = platform.brand)
        assertFalse(actual = platform.model.isBlank())
        assertNotEquals(illegal = "unknown", actual = platform.model)
    }
}
