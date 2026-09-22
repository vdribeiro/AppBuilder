package com.app.builder.ui.push

import kotlin.test.Test
import kotlin.test.assertEquals
import com.app.builder.core.platform.OS
import com.app.builder.core.platform.platform
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

    /** Verifies that [RegisterBroadcastLifecycle] drives the service lifecycle only on the guarded platforms, leaving it untouched everywhere else. */
    @Test
    fun registerBroadcastLifecycleFollowsTheGuardedPlatformSet() = runUITest {
        val service = RecordingBroadcastService()

        setUI { RegisterBroadcastLifecycle(broadcastService = service) }
        waitForIdle()

        assertEquals(expected = expectedEvents(), actual = service.events)
    }

    /** Verifies that [RegisterBroadcastLifecycle] tolerates a null service without throwing. */
    @Test
    fun registerBroadcastLifecycleToleratesNullService() = runUITest {
        setUI { RegisterBroadcastLifecycle(broadcastService = null) }
        waitForIdle()
    }

    /** Verifies that [RegisterPushLifecycle] drives the service lifecycle only on the guarded platforms, leaving it untouched everywhere else. */
    @Test
    fun registerPushLifecycleFollowsTheGuardedPlatformSet() = runUITest {
        val service = RecordingPushService()

        setUI { RegisterPushLifecycle(pushService = service) }
        waitForIdle()

        assertEquals(expected = expectedEvents(), actual = service.events)
    }

    /** Verifies that [RegisterPushLifecycle] tolerates a null service without throwing. */
    @Test
    fun registerPushLifecycleToleratesNullService() = runUITest {
        setUI { RegisterPushLifecycle(pushService = null) }
        waitForIdle()
    }

    /**
     * The lifecycle actions expected once the composable enters composition: the guarded platforms bring the service to the foreground, the rest never touch it.
     *
     * @return The lifecycle actions expected on the current platform.
     */
    private fun expectedEvents(): List<String> = when (platform.os) {
        OS.Android, OS.Ios -> listOf(element = "start")
        OS.Windows, OS.Mac, OS.Linux, OS.Web, OS.Unknown -> emptyList()
    }
}
