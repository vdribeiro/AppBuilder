package com.app.builder.core.telemetry

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.coroutines.flow.first
import com.app.builder.core.config.ClientFlags
import com.app.builder.test.TestCase

class ConsoleTest: TestCase() {

    override suspend fun beforeTest() {
        super.beforeTest()
        ClientFlags.set { it.copy(console = true) }
    }

    /** Verifies info, error, and feedback calls each append a formatted entry to the buffer. */
    @Test
    fun infoErrorFeedbackAppendEntries() = runUnitTest {
        Console.info(tag = "TAG", message = "info message")
        Console.error(tag = "TAG", message = "error message", throwable = null)
        Console.feedback(message = "feedback message")

        val logs = Console.logs.first { it.contains(element = "FEEDBACK: feedback message") }
        assertTrue(actual = logs.contains(element = "INFO [TAG]: info message"))
        assertTrue(actual = logs.contains(element = "ERROR [TAG]: error message"))
    }

    /** Verifies an entry recorded via error includes both the message and the throwable details. */
    @Test
    fun errorIncludesMessageAndThrowable() = runUnitTest {
        val exception = IllegalStateException("boom")
        Console.error(tag = "TAG", message = "failure", throwable = exception)

        val logs = Console.logs.first { it.contains(element = "ERROR [TAG]: failure") }
        val index = logs.indexOf(element = "ERROR [TAG]: failure")
        assertTrue(actual = logs.drop(n = index + 1).any { it.contains(other = "boom") })
    }

    /** Verifies the buffer is cleared when the console flag turns off, retains nothing while it is off, and retains entries again once it turns on. */
    @Test
    fun bufferFollowsTheConsoleFlag() = runUnitTest {
        Console.feedback(message = "before")
        Console.logs.first { it.contains(element = "FEEDBACK: before") }

        ClientFlags.set { it.copy(console = false) }
        Console.logs.first { it.isEmpty() }
        Console.feedback(message = "while off")

        ClientFlags.set { it.copy(console = true) }
        Console.feedback(message = "after")
        val logs = Console.logs.first { it.contains(element = "FEEDBACK: after") }
        assertFalse(actual = logs.contains(element = "FEEDBACK: before"))
        assertFalse(actual = logs.contains(element = "FEEDBACK: while off"))
    }

    /** Verifies [Console.logs] emits the retained lines as they are appended and an empty list once the console flag turns off. */
    @Test
    fun logsFlowFollowsTheBuffer() = runUnitTest {
        Console.feedback(message = "flow")
        Console.logs.first { it.contains(element = "FEEDBACK: flow") }

        ClientFlags.set { it.copy(console = false) }
        Console.logs.first { it.isEmpty() }
    }

    /** Verifies the buffer evicts its oldest entries once more entries than its max size are recorded. */
    @Test
    fun bufferEvictsOldestEntryBeyondMaxSize() = runUnitTest {
        val total = 2500
        repeat(times = total) { i -> Console.feedback(message = "EVICT_$i") }

        val logs = Console.logs.first { it.lastOrNull() == "FEEDBACK: EVICT_${total - 1}" }
        assertEquals(expected = 1000, actual = logs.size)
        assertFalse(actual = logs.contains(element = "FEEDBACK: EVICT_0"))
    }
}
