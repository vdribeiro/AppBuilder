package com.app.builder.data.signal

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import com.app.builder.test.TestCase

class LocalSignalTest: TestCase() {

    /** Verifies that a notification on a channel reaches an observer of that same channel. */
    @Test
    fun notifyReachesObserversOfTheSameChannel() = runServerTest {
        val signal = LocalSignal()
        val received = mutableListOf<String>()

        coroutineScope {
            val job = launch { signal.observe(channel = CHANNEL).collect { received.add(element = it) } }
            awaitUntil { signal.subscriptions.value > 0 }

            assertTrue(actual = signal.notify(channel = CHANNEL, payload = "first"))
            assertTrue(actual = signal.notify(channel = CHANNEL, payload = "second"))
            awaitUntil { received.size == 2 }

            job.cancel()
        }

        assertEquals(expected = listOf("first", "second"), actual = received)
    }

    /** Verifies that notifications on other channels are not delivered to an observer of a different channel. */
    @Test
    fun otherChannelsAreIgnored() = runServerTest {
        val signal = LocalSignal()
        val received = mutableListOf<String>()

        coroutineScope {
            val job = launch { signal.observe(channel = CHANNEL).collect { received.add(element = it) } }
            awaitUntil { signal.subscriptions.value > 0 }

            assertTrue(actual = signal.notify(channel = "other", payload = "ignored"))
            assertTrue(actual = signal.notify(channel = CHANNEL, payload = "delivered"))
            awaitUntil { received.isNotEmpty() }

            job.cancel()
        }

        assertEquals(expected = listOf("delivered"), actual = received)
    }

    /** Verifies that every observer of a channel receives the notified payload. */
    @Test
    fun everyObserverOfAChannelReceivesThePayload() = runServerTest {
        val signal = LocalSignal()
        val first = mutableListOf<String>()
        val second = mutableListOf<String>()

        coroutineScope {
            val firstJob = launch { signal.observe(channel = CHANNEL).collect { first.add(element = it) } }
            val secondJob = launch { signal.observe(channel = CHANNEL).collect { second.add(element = it) } }
            awaitUntil { signal.subscriptions.value > 1 }

            assertTrue(actual = signal.notify(channel = CHANNEL, payload = "payload"))
            awaitUntil { first.isNotEmpty() && second.isNotEmpty() }

            firstJob.cancel()
            secondJob.cancel()
        }

        assertEquals(expected = listOf("payload"), actual = first)
        assertEquals(expected = listOf("payload"), actual = second)
    }

    private companion object {
        /** The channel name used across the test cases. */
        const val CHANNEL = "channel"
    }
}
