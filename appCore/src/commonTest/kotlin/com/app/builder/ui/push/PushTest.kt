package com.app.builder.ui.push

import kotlin.test.Test
import kotlin.test.assertTrue
import com.app.builder.domain.push.BroadcastService
import com.app.builder.domain.push.PushService
import com.app.builder.test.TestCase

class PushTest: TestCase() {

    /** A [BroadcastService] fixture that records every invoked lifecycle action. */
    private class RecordingBroadcastService: BroadcastService {
        /** The lifecycle actions invoked on this fixture, in order. */
        val events = mutableListOf<String>()
        override suspend fun stop() { events.add(element = "stop") }
        override fun start() { events.add(element = "start") }
    }

    /** A [PushService] fixture that records every invoked lifecycle action. */
    private class RecordingPushService: PushService {
        /** The lifecycle actions invoked on this fixture, in order. */
        val events = mutableListOf<String>()
        override suspend fun stop() { events.add(element = "stop") }
        override fun start() { events.add(element = "start") }
    }

    /** Verifies that [RegisterBroadcastLifecycle] does not drive the service lifecycle on this desktop platform, which is excluded from the guarded platform set. */
    @Test
    fun registerBroadcastLifecycleIsANoOpOnDesktop() = runUITest {
        val service = RecordingBroadcastService()

        setUI { RegisterBroadcastLifecycle(broadcastService = service) }
        waitForIdle()

        assertTrue(actual = service.events.isEmpty())
    }

    /** Verifies that [RegisterBroadcastLifecycle] tolerates a null service without throwing. */
    @Test
    fun registerBroadcastLifecycleToleratesNullService() = runUITest {
        setUI { RegisterBroadcastLifecycle(broadcastService = null) }
        waitForIdle()
    }

    /** Verifies that [RegisterPushLifecycle] does not drive the service lifecycle on this desktop platform, which is excluded from the guarded platform set. */
    @Test
    fun registerPushLifecycleIsANoOpOnDesktop() = runUITest {
        val service = RecordingPushService()

        setUI { RegisterPushLifecycle(pushService = service) }
        waitForIdle()

        assertTrue(actual = service.events.isEmpty())
    }

    /** Verifies that [RegisterPushLifecycle] tolerates a null service without throwing. */
    @Test
    fun registerPushLifecycleToleratesNullService() = runUITest {
        setUI { RegisterPushLifecycle(pushService = null) }
        waitForIdle()
    }
}
