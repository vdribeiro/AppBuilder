package com.app.builder.core.devicelocation

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.uuid.Uuid
import com.app.builder.core.config.ClientFlags
import com.app.builder.test.TestCase

class DeviceLocationProviderTest: TestCase() {

    /** A fixture [DeviceLocation] used to exercise capture updates. */
    private fun fixtureLocation(): DeviceLocation = DeviceLocation(
        uuid = Uuid.random(),
        provider = "fused",
        fixTime = 0L,
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

    /** Verifies that the shared [DeviceLocationProvider.instance] is a valid, non-crashing default. */
    @Test
    fun sharedInstanceIsUsable() = runUnitTest {
        DeviceLocationProvider.instance.stopUpdate()
        assertEquals(expected = DeviceLocationProvider.State.Idle, actual = DeviceLocationProvider.instance.state.value)
    }
}
