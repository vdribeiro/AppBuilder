package com.app.builder.core.platform

import kotlin.test.Test
import kotlin.test.assertNull
import kotlin.test.assertTrue
import com.app.builder.test.TestCase

class ResourceTest: TestCase() {

    /** Verifies that reading an existing resource returns its text. */
    @Test
    fun readResource() = runServerTest {
        assertTrue(actual = readResource(path = "static/translations.json").orEmpty().isNotBlank())
    }

    /** Verifies that reading a missing resource returns null. */
    @Test
    fun readMissingResource() = runServerTest {
        assertNull(actual = readResource(path = "static/missing.json"))
    }
}
