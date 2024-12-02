package com.app.builder.core.telemetry

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import com.app.builder.test.TestCase

class ConsoleTest: TestCase() {

    /** Verifies info, error, and feedback calls each append a formatted entry to the buffer. */
    @Test
    fun infoErrorFeedbackAppendEntries() = runUnitTest {
        Console.info(tag = "TAG", message = "info message")
        Console.error(tag = "TAG", message = "error message", throwable = null)
        Console.feedback(message = "feedback message")

        val snapshot = Console.getSnapshot()
        assertTrue(actual = snapshot.contains(element = "INFO [TAG]: info message"))
        assertTrue(actual = snapshot.contains(element = "ERROR [TAG]: error message"))
        assertTrue(actual = snapshot.contains(element = "FEEDBACK: feedback message"))
    }

    /** Verifies an entry recorded via error includes both the message and the throwable details. */
    @Test
    fun errorIncludesMessageAndThrowable() = runUnitTest {
        val exception = IllegalStateException("boom")
        Console.error(tag = "TAG", message = "failure", throwable = exception)

        val snapshot = Console.getSnapshot()
        val index = snapshot.indexOf(element = "ERROR [TAG]: failure")
        assertTrue(actual = index >= 0)
        assertTrue(actual = snapshot.drop(n = index + 1).any { it.contains(other = "boom") })
    }

    /** Verifies the buffer evicts its oldest entries once more entries than its max size are recorded. */
    @Test
    fun bufferEvictsOldestEntryBeyondMaxSize() = runUnitTest {
        val total = 2500
        repeat(times = total) { i -> Console.feedback(message = "EVICT_$i") }

        val snapshot = Console.getSnapshot()
        assertEquals(expected = 1000, actual = snapshot.size)
        assertFalse(actual = snapshot.contains(element = "FEEDBACK: EVICT_0"))
        assertEquals(expected = "FEEDBACK: EVICT_${total - 1}", actual = snapshot.last())
    }
}
