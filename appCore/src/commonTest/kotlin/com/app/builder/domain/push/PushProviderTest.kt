package com.app.builder.domain.push

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.uuid.Uuid
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import com.app.builder.core.config.ClientConfigs
import com.app.builder.core.flow.Dispatcher
import com.app.builder.domain.EntityType
import com.app.builder.domain.PushPayload
import com.app.builder.domain.deeplink.DeepLink
import com.app.builder.test.FakeData
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

    /** Verifies that once more than [ClientConfigs.maxHandledPayloads] payloads were handled, the oldest uuid is forgotten and handled again while the newer ones are still ignored. */
    @Test
    fun handledPayloadHistoryEvictsTheOldestUuid() = runUnitTest {
        ClientConfigs.set { it.copy(maxHandledPayloads = 2) }
        val first = PushPayload.Flags()
        val second = PushPayload.Flags()
        val third = PushPayload.Flags()

        listOf(first, second, third).forEach { assertTrue(actual = PushProvider.handlePushPayload(pushPayload = it)) }

        assertFalse(actual = PushProvider.handlePushPayload(pushPayload = third))
        assertFalse(actual = PushProvider.handlePushPayload(pushPayload = second))
        assertTrue(actual = PushProvider.handlePushPayload(pushPayload = first))
    }

    /** Verifies that [PushProvider.reset] clears the token, forgets handled payloads and discards payloads buffered but not yet collected. */
    @Test
    fun resetClearsTokenHistoryAndBufferedPayloads() = runUnitTest {
        val buffered = PushPayload.Flags()
        PushProvider.setToken(token = "token")
        PushProvider.handlePushPayload(pushPayload = buffered)

        PushProvider.reset()

        assertNull(actual = PushProvider.token.value)
        val next = PushPayload.Configs()
        assertTrue(actual = PushProvider.handlePushPayload(pushPayload = next))
        assertEquals(expected = next, actual = PushProvider.pushPayloads.first())
        assertTrue(actual = PushProvider.handlePushPayload(pushPayload = buffered))
    }

    /** Verifies that clicking an entity notification dispatches a deep link to the targeted entity. */
    @Test
    fun entityNotificationClickSendsEntityDeepLink() = runUnitTest {
        val notification = FakeData.entityNotification.copy(entityUuid = Uuid.random())

        PushProvider.handleNotificationClick(pushPayload = notification)

        assertEquals(
            expected = DeepLink.Entity(entityUuid = notification.entityUuid, entityType = EntityType.TRANSLATION),
            actual = DeepLink.deepLinks.first()
        )
    }

    /** Verifies that clicking a payload that does not target an entity dispatches no deep link. */
    @Test
    fun nonEntityNotificationClickSendsNoDeepLink() = runUnitTest {
        val received = mutableListOf<DeepLink>()
        backgroundScope.launch(context = Dispatcher.Default) { DeepLink.deepLinks.collect { received.add(element = it) } }

        listOf(FakeData.notification, PushPayload.Flags(), PushPayload.Configs(), PushPayload.Reset()).forEach {
            PushProvider.handleNotificationClick(pushPayload = it)
        }
        testScheduler.advanceUntilIdle()

        assertTrue(actual = received.isEmpty())
    }
}
