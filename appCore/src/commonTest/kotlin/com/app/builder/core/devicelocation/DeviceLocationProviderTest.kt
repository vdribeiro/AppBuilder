package com.app.builder.core.devicelocation

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.uuid.Uuid
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.delay
import com.app.builder.core.config.ClientConfigs
import com.app.builder.core.config.ClientFlags
import com.app.builder.test.TestCase

class DeviceLocationProviderTest: TestCase() {

    /**
     * A fixture [DeviceLocation] used to exercise capture updates.
     *
     * @return The fixture [DeviceLocation].
     */
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

    /**
     * A [DeviceLocationProvider] that reports itself as available and permitted, so the public lifecycle methods are not short-circuited by their guards.
     *
     * @return A [DeviceLocationProvider] that always reports itself available and permitted.
     */
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

    /** Verifies that a platform query which never returns is abandoned at the timeout and answered from the last captured fix instead of suspending the caller indefinitely. */
    @Test
    fun getLastKnownLocationTimesOutAndFallsBackToTheLastCapturedFix() = runUnitTest {
        ClientFlags.set { it.copy(locationCapture = true) }
        val fixture = fixtureLocation()
        var returnedFixture = true
        val provider = object: DeviceLocationProvider() {
            override val available: Boolean = true
            override fun hasPermission(): Boolean = true
            override suspend fun platformGetLastKnownLocation(): DeviceLocation? {
                if (returnedFixture) return fixture
                awaitCancellation()
            }
        }
        assertEquals(expected = fixture, actual = provider.getLastKnownLocation())

        returnedFixture = false

        assertEquals(expected = fixture, actual = provider.getLastKnownLocation())
    }

    /** Verifies that a platform query which never returns yields null when no fix was ever captured, rather than hanging. */
    @Test
    fun getLastKnownLocationTimesOutToNullWithoutACapturedFix() = runUnitTest {
        ClientFlags.set { it.copy(locationCapture = true) }
        val provider = object: DeviceLocationProvider() {
            override val available: Boolean = true
            override fun hasPermission(): Boolean = true
            override suspend fun platformGetLastKnownLocation(): DeviceLocation? = awaitCancellation()
        }

        assertNull(actual = provider.getLastKnownLocation())
    }

    /** Verifies that a platform query which returns within the timeout is used as-is. */
    @Test
    fun getLastKnownLocationUsesASlowButTimelyPlatformAnswer() = runUnitTest {
        ClientFlags.set { it.copy(locationCapture = true) }
        val fixture = fixtureLocation()
        val provider = object: DeviceLocationProvider() {
            override val available: Boolean = true
            override fun hasPermission(): Boolean = true
            override suspend fun platformGetLastKnownLocation(): DeviceLocation? {
                delay(timeMillis = ClientConfigs.configs.locationQueryTimeoutMillis - 1)
                return fixture
            }
        }

        assertEquals(expected = fixture, actual = provider.getLastKnownLocation())
    }

    /** Verifies that starting an update is ignored while the provider is unavailable, even with the feature flag enabled. */
    @Test
    fun startUpdateIgnoredWhileUnavailable() = runUnitTest {
        ClientFlags.set { it.copy(locationCapture = true) }
        val provider = object: DeviceLocationProvider() {
            override fun hasPermission(): Boolean = true
        }

        provider.startUpdate()

        assertEquals(expected = DeviceLocationProvider.State.Idle, actual = provider.state.value)
    }

    /** Verifies that starting an update is ignored while location permission is not granted. */
    @Test
    fun startUpdateIgnoredWithoutPermission() = runUnitTest {
        ClientFlags.set { it.copy(locationCapture = true) }
        val provider = object: DeviceLocationProvider() {
            override val available: Boolean = true
        }

        provider.startUpdate()

        assertEquals(expected = DeviceLocationProvider.State.Idle, actual = provider.state.value)
    }

    /** Verifies that an exception thrown by the platform's start call is caught, stops the platform update, and leaves the provider idle. */
    @Test
    fun exceptionFromPlatformStartUpdateIsCaught() = runUnitTest {
        ClientFlags.set { it.copy(locationCapture = true) }
        val events = mutableListOf<String>()
        val provider = object: DeviceLocationProvider() {
            override val available: Boolean = true
            override fun hasPermission(): Boolean = true
            override fun platformStartUpdate() {
                events.add(element = "start")
                throw IllegalStateException("boom")
            }

            override fun platformStopUpdate() {
                events.add(element = "stop")
            }
        }

        provider.startUpdate()

        assertEquals(expected = listOf("stop", "start", "stop"), actual = events)
        assertEquals(expected = DeviceLocationProvider.State.Idle, actual = provider.state.value)
    }

    /** Verifies that a failed restart of an active provider transitions it back to idle, since the platform update was stopped. */
    @Test
    fun failedRestartTransitionsActiveProviderToIdle() = runUnitTest {
        ClientFlags.set { it.copy(locationCapture = true) }
        var failStart = false
        val provider = object: DeviceLocationProvider() {
            override val available: Boolean = true
            override fun hasPermission(): Boolean = true
            override fun platformStartUpdate() {
                if (failStart) throw IllegalStateException("boom")
            }
        }
        provider.startUpdate()
        assertEquals(expected = DeviceLocationProvider.State.Active, actual = provider.state.value)
        failStart = true

        provider.startUpdate()

        assertEquals(expected = DeviceLocationProvider.State.Idle, actual = provider.state.value)
    }

    /** Verifies that starting an update whose platform stop call always throws is caught without escaping and leaves the provider idle. */
    @Test
    fun exceptionFromPlatformStopUpdateDuringStartIsCaught() = runUnitTest {
        ClientFlags.set { it.copy(locationCapture = true) }
        val provider = object: DeviceLocationProvider() {
            override val available: Boolean = true
            override fun hasPermission(): Boolean = true
            override fun platformStopUpdate() {
                throw IllegalStateException("boom")
            }
        }

        provider.startUpdate()

        assertEquals(expected = DeviceLocationProvider.State.Idle, actual = provider.state.value)
    }

    /** Verifies that an exception thrown by the platform's stop call is caught and the provider still transitions to idle. */
    @Test
    fun exceptionFromPlatformStopUpdateIsCaught() = runUnitTest {
        ClientFlags.set { it.copy(locationCapture = true) }
        var failStop = false
        val provider = object: DeviceLocationProvider() {
            override val available: Boolean = true
            override fun hasPermission(): Boolean = true
            override fun platformStopUpdate() {
                if (failStop) throw IllegalStateException("boom")
            }
        }
        provider.startUpdate()
        failStop = true

        provider.stopUpdate()

        assertEquals(expected = DeviceLocationProvider.State.Idle, actual = provider.state.value)
    }

    /** Verifies that [DeviceLocationProvider.getLastKnownLocation] returns null and stops updates while the feature flag is disabled. */
    @Test
    fun getLastKnownLocationReturnsNullWhileFeatureFlagDisabled() = runUnitTest {
        ClientFlags.set { it.copy(locationCapture = true) }
        val provider = testProvider()
        provider.startUpdate()
        ClientFlags.set { it.copy(locationCapture = false) }

        assertNull(actual = provider.getLastKnownLocation())
        assertEquals(expected = DeviceLocationProvider.State.Idle, actual = provider.state.value)
    }

    /** Verifies that [DeviceLocationProvider.getLastKnownLocation] returns null while location permission is not granted. */
    @Test
    fun getLastKnownLocationReturnsNullWithoutPermission() = runUnitTest {
        ClientFlags.set { it.copy(locationCapture = true) }
        val provider = object: DeviceLocationProvider() {
            override val available: Boolean = true
        }

        assertNull(actual = provider.getLastKnownLocation())
    }

    /** Verifies that a provider relying on the default platform query, which supports no cached location, returns null. */
    @Test
    fun getLastKnownLocationReturnsNullWithDefaultPlatformQuery() = runUnitTest {
        ClientFlags.set { it.copy(locationCapture = true) }

        assertNull(actual = testProvider().getLastKnownLocation())
    }

    /** Verifies that an exception thrown by the platform query is caught and answered from the last captured fix. */
    @Test
    fun exceptionFromPlatformQueryFallsBackToTheLastCapturedFix() = runUnitTest {
        ClientFlags.set { it.copy(locationCapture = true) }
        val fixture = fixtureLocation()
        var fail = false
        val provider = object: DeviceLocationProvider() {
            override val available: Boolean = true
            override fun hasPermission(): Boolean = true
            override suspend fun platformGetLastKnownLocation(): DeviceLocation? {
                if (fail) throw IllegalStateException("boom")
                return fixture
            }
        }
        assertEquals(expected = fixture, actual = provider.getLastKnownLocation())

        fail = true

        assertEquals(expected = fixture, actual = provider.getLastKnownLocation())
    }

    /** Verifies that the top-level [getLastKnownLocation] returns null while the location capture feature flag is disabled. */
    @Test
    fun topLevelGetLastKnownLocationReturnsNullWhileFeatureFlagDisabled() = runUnitTest {
        assertNull(actual = getLastKnownLocation())
    }

    /** Verifies that the shared [DeviceLocationProvider.instance] is a valid, non-crashing default. */
    @Test
    fun sharedInstanceIsUsable() = runUnitTest {
        DeviceLocationProvider.instance.stopUpdate()
        assertEquals(expected = DeviceLocationProvider.State.Idle, actual = DeviceLocationProvider.instance.state.value)
    }
}
