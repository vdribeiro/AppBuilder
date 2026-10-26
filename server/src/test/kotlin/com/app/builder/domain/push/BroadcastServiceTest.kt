package com.app.builder.domain.push

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeout
import com.app.builder.core.config.ServerConfigs
import com.app.builder.core.config.ServerFlags
import com.app.builder.data.http.FakeSSESession
import com.app.builder.domain.PushPayload
import com.app.builder.test.TestCase

class BroadcastServiceTest: TestCase() {

    /** Verifies that connections can be added and removed, respecting the configured connection limit. */
    @Test
    fun addRemoveConnection() = runServerTest {
        ServerConfigs.set { it.copy(broadcastMaxConnections = 1) }
        val broadcastService = dependency.get().broadcastService
        val session = FakeSSESession()

        assertTrue(actual = broadcastService.addConnection(session = session))
        assertTrue(actual = broadcastService.removeConnection(session = session))
        assertTrue(actual = broadcastService.addConnection(session = session))
        assertFalse(actual = broadcastService.addConnection(session = FakeSSESession()))
    }

    /** Verifies that a broadcast is only delivered to connected sessions and does not fail on dead ones. */
    @Test
    fun broadcast() = runServerTest {
        val broadcastService = dependency.get().broadcastService
        val session = FakeSSESession()
        val deadSession = FakeSSESession(fail = true)
        broadcastService.start()

        broadcastService.broadcast(pushPayload = PushPayload.Flags())
        assertFalse(actual = session.sent)
        assertFalse(actual = deadSession.sent)

        broadcastService.addConnection(session = session)
        broadcastService.addConnection(session = deadSession)
        broadcastService.broadcast(pushPayload = PushPayload.Flags())
        awaitUntil { session.sent }
        assertTrue(actual = session.sent)
        assertFalse(actual = deadSession.sent)
        broadcastService.stop()
    }

    /** Verifies that a local broadcast is delivered to a connected session exactly once. */
    @Test
    fun broadcastDeliversLocallyExactlyOnce() = runServerTest {
        val broadcastService = dependency.get().broadcastService
        val session = FakeSSESession()
        broadcastService.start()
        broadcastService.addConnection(session = session)

        broadcastService.broadcast(pushPayload = PushPayload.Flags())
        awaitUntil { session.sent }

        delay(timeMillis = 200)
        assertEquals(expected = 1, actual = session.sentCount)
        broadcastService.stop()
    }

    /** Verifies that a broadcast sent from another server instance reaches this instance's connected sessions. */
    @Test
    fun broadcastReachesSessionsOfOtherInstances() = runServerTest {
        val broadcastService = dependency.get().broadcastService
        val session = FakeSSESession()
        broadcastService.start()
        broadcastService.addConnection(session = session)

        val otherInstance = BroadcastManager(instanceSignal = dependency.get().instanceSignal, fcmService = NoOpFcmService)
        otherInstance.broadcast(pushPayload = PushPayload.Flags())

        awaitUntil { session.sent }
        assertEquals(expected = 1, actual = session.sentCount)
        broadcastService.stop()
    }

    /** Verifies that a broadcast is also relayed to the FCM broadcast topic, so devices without an open stream receive it. */
    @Test
    fun broadcastIsRelayedToTheFcmTopic() = runServerTest {
        val fakeFcmService = FakeFcmService()
        val broadcastService = BroadcastManager(instanceSignal = dependency.get().instanceSignal, fcmService = fakeFcmService)
        val pushPayload = PushPayload.Flags()

        broadcastService.broadcast(pushPayload = pushPayload)

        assertEquals(expected = pushPayload, actual = withTimeout(timeMillis = 5_000L) { fakeFcmService.broadcasts.receive() })
    }

    /** Verifies that a session failing every delivery attempt is dropped, freeing its slot for a new connection. */
    @Test
    fun failingSessionIsDropped() = runServerTest {
        ServerConfigs.set { it.copy(broadcastMaxConnections = 1, broadcastMaxAttempts = 1, broadcastRetryDelay = 1L) }
        val broadcastService = dependency.get().broadcastService
        val deadSession = FakeSSESession(fail = true)

        assertTrue(actual = broadcastService.addConnection(session = deadSession))
        assertFalse(actual = broadcastService.addConnection(session = FakeSSESession()))

        broadcastService.broadcast(pushPayload = PushPayload.Flags())
        awaitUntil { broadcastService.addConnection(session = FakeSSESession()) }
        assertFalse(actual = broadcastService.removeConnection(session = deadSession))
    }

    /** Verifies that starting the service is a no-op while the broadcast flag is off, so broadcasts from other instances are not relayed to local sessions. */
    @Test
    fun startIsNoOpWhenBroadcastIsDisabled() = runServerTest {
        ServerFlags.set { it.copy(broadcast = false) }
        val broadcastService = dependency.get().broadcastService
        val session = FakeSSESession()
        broadcastService.start()
        broadcastService.addConnection(session = session)

        val otherInstance = BroadcastManager(instanceSignal = dependency.get().instanceSignal, fcmService = NoOpFcmService)
        otherInstance.broadcast(pushPayload = PushPayload.Flags())

        delay(timeMillis = 200)
        assertFalse(actual = session.sent)
        broadcastService.stop()
    }
}
