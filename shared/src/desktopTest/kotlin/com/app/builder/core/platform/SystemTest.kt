package com.app.builder.core.platform

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.milliseconds
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

    /** Verifies runCommand returns the standard output of a successful command. */
    @Test
    fun runCommandReturnsOutput() = runUnitTest {
        if (isWindows()) return@runUnitTest
        assertEquals(expected = "hello\n", actual = runCommand(command = listOf("echo", "hello")))
    }

    /** Verifies runCommand writes its input to the process standard input. */
    @Test
    fun runCommandWritesInput() = runUnitTest {
        if (isWindows()) return@runUnitTest
        assertEquals(expected = "secret", actual = runCommand(command = listOf("cat"), input = "secret"))
    }

    /** Verifies runCommand returns null for a non-zero exit status, discarding standard error. */
    @Test
    fun runCommandReturnsNullOnFailure() = runUnitTest {
        if (isWindows()) return@runUnitTest
        assertNull(actual = runCommand(command = listOf("sh", "-c", "echo out; echo err >&2; exit 3")))
    }

    /** Verifies runCommand returns null for a command that does not exist. */
    @Test
    fun runCommandReturnsNullForMissingCommand() = runUnitTest {
        assertNull(actual = runCommand(command = listOf("not-a-real-command-xyz")))
    }

    /** Verifies runCommand stops a process that outlives the timeout and returns null. */
    @Test
    fun runCommandStopsAStalledProcess() = runUnitTest {
        if (isWindows()) return@runUnitTest
        assertNull(actual = runCommand(command = listOf("sleep", "5"), timeout = 200.milliseconds))
    }

    /**
     * Whether the tests run on Windows, where the POSIX commands used here are unavailable.
     *
     * @return `true` on Windows.
     */
    private fun isWindows(): Boolean = when (platform.os) {
        OS.Windows -> true
        OS.Android, OS.Ios, OS.Mac, OS.Linux, OS.Web, OS.Unknown -> false
    }

    /** Verifies that [machineId] reads a non-blank identifier and returns the same one on every call. */
    @Test
    fun machineIdIsReadAndStable() = runUnitTest {
        val first = assertNotNull(actual = machineId())

        assertTrue(actual = first.concatToString().isNotBlank())
        assertEquals(expected = first.concatToString(), actual = machineId()?.concatToString())
    }
}
