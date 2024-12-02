package com.app.builder.core.devicelocation

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.uuid.Uuid
import kotlinx.coroutines.test.advanceTimeBy
import com.app.builder.core.config.ClientConfigs
import com.app.builder.core.config.ClientFlags
import com.app.builder.core.locale.now
import com.app.builder.test.TestCase

class DeviceLocationProviderTest: TestCase() {

    /**
     * A fixture [DeviceLocation] used to exercise capture updates.
     *
     * @param fixTime The Unix epoch time the fix was acquired at.
     */
    private fun fixtureLocation(fixTime: Long = 0L): DeviceLocation = DeviceLocation(
        uuid = Uuid.random(),
        provider = "fused",
        fixTime = fixTime,
        deviceTime = 0L,
        latitude = 0.0,
        longitude = 0.0,
        altitude = null,
        accuracy = 0.0,
        bearing = null,
        speed = null,
    )

    /** A [DeviceLocationProvider] that reports itself as available and permitted, so the public lifecycle methods are not short-circuited by their guards. */
    private fun testProvider(): DeviceLocationProvider = object: DeviceLocationProvider() {
        override val available: Boolean = true
        override fun hasPermission(): Boolean = true
    }

    /** A [DeviceLocationProvider] that records the cadence of every platform start, so the burst-to-steady transition can be observed. */
    private class RecordingProvider: DeviceLocationProvider() {
        /** Every [Mode] the platform loop has been started at, in order. */
        val modes: MutableList<Mode> = mutableListOf()

        override val available: Boolean = true
        override fun hasPermission(): Boolean = true
        override fun platformStartUpdate(mode: Mode) {
            modes.add(element = mode)
        }

        /**
         * Delivers a fix as if the platform loop had captured it.
         *
         * @param deviceLocation The fix to deliver.
         */
        fun capture(deviceLocation: DeviceLocation) = setLastKnownLocation(deviceLocation = deviceLocation)
    }

    /** Verifies that a new [DeviceLocationProvider] starts in the idle state. */
    @Test
    fun initialStateIsIdle() = runUnitTest {
        val provider = DeviceLocationProvider()
        assertEquals(expected = DeviceLocationProvider.State.Idle, actual = provider.state.value)
    }

    /** Verifies that [DeviceLocationProvider.hasPermission] defaults to false. */
    @Test
    fun hasPermissionDefaultsToFalse() = runUnitTest {
        val provider = DeviceLocationProvider()
        assertEquals(expected = false, actual = provider.hasPermission())
    }

    /** Verifies that starting an update is ignored while the location capture feature flag is disabled. */
    @Test
    fun startUpdateIgnoredWhileFeatureFlagDisabled() = runUnitTest {
        val provider = testProvider()

        provider.startUpdate()

        assertEquals(expected = DeviceLocationProvider.State.Idle, actual = provider.state.value)
    }

    /** Verifies that starting an update transitions to active once the location capture feature flag is enabled. */
    @Test
    fun startUpdateTransitionsToActiveWhenEnabled() = runUnitTest {
        ClientFlags.set { it.copy(locationCapture = true) }
        val provider = testProvider()

        provider.startUpdate()

        assertEquals(expected = DeviceLocationProvider.State.Active, actual = provider.state.value)
    }

    /** Verifies that stopping an update transitions the provider back to idle. */
    @Test
    fun stopUpdateTransitionsToIdle() = runUnitTest {
        ClientFlags.set { it.copy(locationCapture = true) }
        val provider = testProvider()
        provider.startUpdate()

        provider.stopUpdate()

        assertEquals(expected = DeviceLocationProvider.State.Idle, actual = provider.state.value)
    }

    /** Verifies that [DeviceLocationProvider.getLastKnownLocation] returns null while the provider is unavailable. */
    @Test
    fun getLastKnownLocationReturnsNullWhenUnavailable() = runUnitTest {
        ClientFlags.set { it.copy(locationCapture = true) }
        val provider = DeviceLocationProvider()

        assertNull(actual = provider.getLastKnownLocation())
    }

    /** Verifies that [DeviceLocationProvider.getLastKnownLocation] caches a platform-provided fix and falls back to it once the platform stops providing one. */
    @Test
    fun getLastKnownLocationFallsBackToLastCapturedFix() = runUnitTest {
        ClientFlags.set { it.copy(locationCapture = true) }
        val fixture = fixtureLocation()
        var platformResult: DeviceLocation? = fixture
        val provider = object: DeviceLocationProvider() {
            override val available: Boolean = true
            override fun hasPermission(): Boolean = true
            override suspend fun platformGetLastKnownLocation(): DeviceLocation? = platformResult
        }

        assertEquals(expected = fixture, actual = provider.getLastKnownLocation())

        platformResult = null
        assertEquals(expected = fixture, actual = provider.getLastKnownLocation())
    }

    /** Verifies that a start opens the platform loop with a burst, so a stale platform cache is re-warmed before the steady cadence takes over. */
    @Test
    fun startUpdateOpensWithABurst() = runUnitTest {
        ClientFlags.set { it.copy(locationCapture = true) }
        val provider = RecordingProvider()

        provider.startUpdate()

        assertEquals(expected = listOf(DeviceLocationProvider.Mode.Burst), actual = provider.modes)
    }

    /** Verifies that the burst settles to the steady cadence once a fix acquired after the start lands. */
    @Test
    fun burstSettlesToSteadyOnAFreshFix() = runUnitTest {
        ClientFlags.set { it.copy(locationCapture = true) }
        val provider = RecordingProvider()
        provider.startUpdate()

        provider.capture(deviceLocation = fixtureLocation(fixTime = now().toEpochMilliseconds()))

        assertEquals(expected = listOf(DeviceLocationProvider.Mode.Burst, DeviceLocationProvider.Mode.Steady), actual = provider.modes)
        assertEquals(expected = DeviceLocationProvider.State.Active, actual = provider.state.value)
    }

    /** Verifies that a fix acquired before the start, as one replayed out of the platform's stale cache would be, does not end the burst early. */
    @Test
    fun burstIgnoresAStaleFix() = runUnitTest {
        ClientFlags.set { it.copy(locationCapture = true) }
        val provider = RecordingProvider()
        provider.startUpdate()

        provider.capture(deviceLocation = fixtureLocation(fixTime = now().toEpochMilliseconds() - 1))

        assertEquals(expected = listOf(DeviceLocationProvider.Mode.Burst), actual = provider.modes)
    }

    /** Verifies that the burst settles to the steady cadence once it times out, so a device that never gets a fix is not left burning power at the burst cadence. */
    @Test
    fun burstSettlesToSteadyOnTimeout() = runUnitTest {
        ClientFlags.set { it.copy(locationCapture = true) }
        val provider = RecordingProvider()
        provider.startUpdate()

        advanceTimeBy(delayTimeMillis = ClientConfigs.configs.locationBurstTimeoutMillis + 1)

        assertEquals(expected = listOf(DeviceLocationProvider.Mode.Burst, DeviceLocationProvider.Mode.Steady), actual = provider.modes)
    }

    /** Verifies that a paused provider resumes with a fresh burst, since the platform cache goes stale while the app is backgrounded. */
    @Test
    fun resumeUpdateRestartsAPausedProviderWithABurst() = runUnitTest {
        ClientFlags.set { it.copy(locationCapture = true) }
        val provider = RecordingProvider()
        provider.startUpdate()
        provider.capture(deviceLocation = fixtureLocation(fixTime = now().toEpochMilliseconds()))

        provider.pauseUpdate()
        assertEquals(expected = DeviceLocationProvider.State.Idle, actual = provider.state.value)

        provider.resumeUpdate()

        assertEquals(expected = DeviceLocationProvider.State.Active, actual = provider.state.value)
        assertEquals(expected = DeviceLocationProvider.Mode.Burst, actual = provider.modes.last())
    }

    /** Verifies that a resume does not restart a provider that was already idle when it was paused. */
    @Test
    fun resumeUpdateIgnoresAProviderThatWasNotCapturing() = runUnitTest {
        ClientFlags.set { it.copy(locationCapture = true) }
        val provider = RecordingProvider()

        provider.pauseUpdate()
        provider.resumeUpdate()

        assertEquals(expected = DeviceLocationProvider.State.Idle, actual = provider.state.value)
        assertTrue(actual = provider.modes.isEmpty())
    }

    /** Verifies that an explicit stop is not undone by a later resume, so capture stays off until something asks for it again. */
    @Test
    fun resumeUpdateIgnoresAnExplicitStop() = runUnitTest {
        ClientFlags.set { it.copy(locationCapture = true) }
        val provider = RecordingProvider()
        provider.startUpdate()

        provider.stopUpdate()
        provider.resumeUpdate()

        assertEquals(expected = DeviceLocationProvider.State.Idle, actual = provider.state.value)
    }

    /** Verifies that the shared [DeviceLocationProvider.instance] is a valid, non-crashing default. */
    @Test
    fun sharedInstanceIsUsable() = runUnitTest {
        DeviceLocationProvider.instance.stopUpdate()
        assertEquals(expected = DeviceLocationProvider.State.Idle, actual = DeviceLocationProvider.instance.state.value)
    }
}
