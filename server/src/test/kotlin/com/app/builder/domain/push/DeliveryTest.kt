package com.app.builder.domain.push

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import com.app.builder.test.TestCase

class DeliveryTest: TestCase() {

    /** Verifies that a successful first attempt stops further retries. */
    @Test
    fun firstSuccessfulAttemptStopsRetrying() = runServerTest {
        var attempts = 0

        val delivered = sendWithRetry(maxAttempts = 3, retryDelay = 1L) { attempts++ }

        assertTrue(actual = delivered)
        assertEquals(expected = 1, actual = attempts)
    }

    /** Verifies that delivery keeps retrying after transient failures until an attempt succeeds. */
    @Test
    fun retriesUntilAnAttemptSucceeds() = runServerTest {
        var attempts = 0

        val delivered = sendWithRetry(maxAttempts = 3, retryDelay = 1L) {
            attempts++
            if (attempts < 3) error(message = "transient failure")
        }

        assertTrue(actual = delivered)
        assertEquals(expected = 3, actual = attempts)
    }

    /** Verifies that delivery gives up and reports failure once every attempt fails. */
    @Test
    fun givesUpAfterEveryAttemptFails() = runServerTest {
        var attempts = 0

        val delivered = sendWithRetry(maxAttempts = 3, retryDelay = 1L) {
            attempts++
            error(message = "permanent failure")
        }

        assertFalse(actual = delivered)
        assertEquals(expected = 3, actual = attempts)
    }

    /** Verifies that a max attempts count of zero results in no delivery and no attempts made. */
    @Test
    fun noAttemptsMeansNoDelivery() = runServerTest {
        var attempts = 0

        val delivered = sendWithRetry(maxAttempts = 0, retryDelay = 1L) { attempts++ }

        assertFalse(actual = delivered)
        assertEquals(expected = 0, actual = attempts)
    }
}
