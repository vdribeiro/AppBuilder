package com.app.builder.core.platform

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import com.app.builder.test.TestCase

class SystemTest: TestCase() {

    /** Verifies getProperty returns the same value as the JDK System.getProperty for a known key. */
    @Test
    fun getPropertyReturnsKnownValue() = runUnitTest {
        assertEquals(expected = System.getProperty("os.name"), actual = getProperty(name = "os.name"))
    }

    /** Verifies getProperty returns null for an unknown property key. */
    @Test
    fun getPropertyReturnsNullForUnknownKey() = runUnitTest {
        assertNull(actual = getProperty(name = "not.a.real.property"))
    }

    /** Verifies getEnv returns the same value as the JDK System.getenv for a known key. */
    @Test
    fun getEnvReturnsKnownValue() = runUnitTest {
        val key = System.getenv().keys.first()
        assertEquals(expected = System.getenv(key), actual = getEnv(name = key))
    }

    /** Verifies getEnv returns null for an unknown environment variable name. */
    @Test
    fun getEnvReturnsNullForUnknownKey() = runUnitTest {
        assertNull(actual = getEnv(name = "NOT_A_REAL_ENV_VAR"))
    }
}