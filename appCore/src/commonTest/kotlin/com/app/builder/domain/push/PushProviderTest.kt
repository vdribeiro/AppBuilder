package com.app.builder.domain.push

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.launch
import com.app.builder.core.flow.Dispatcher
import com.app.builder.domain.PushPayload
import com.app.builder.test.TestCase

class PushProviderTest: TestCase() {

    /** Verifies that setting and clearing the push token updates [PushProvider.token]. */
    @Test
    fun setTokenUpdatesAndClearsToken() = runUnitTest {
        assertNull(actual = PushProvider.token.value)

        PushProvider.setToken(token = "token")
        assertEquals(expected = "token", actual = PushProvider.token.value)

        PushProvider.setToken(token = null)
        assertNull(actual = PushProvider.token.value)
    }

    /** Verifies that handling a push payload delivers it through [PushProvider.pushPayloads]. */
    @Test
    fun handlePushPayloadEmitsToPushPayloads() = runUnitTest {
        val received = mutableListOf<PushPayload>()
        backgroundScope.launch(context = Dispatcher.Default) { PushProvider.pushPayloads.collect { received.add(element = it) } }

        val payload = PushPayload.Flags()
        assertTrue(actual = PushProvider.handlePushPayload(pushPayload = payload))
        testScheduler.advanceUntilIdle()

        assertEquals(expected = listOf<PushPayload>(payload), actual = received)
    }

    /** Verifies that handling the same payload uuid twice is ignored the second time. */
    @Test
    fun duplicatePushPayloadIsIgnored() = runUnitTest {
        val payload = PushPayload.Flags()

        assertTrue(actual = PushProvider.handlePushPayload(pushPayload = payload))
        assertFalse(actual = PushProvider.handlePushPayload(pushPayload = payload))
    }
}
