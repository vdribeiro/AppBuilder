package com.app.builder.core.devicelocation

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import com.app.builder.core.config.ClientFlags
import com.app.builder.core.config.ClientConfigs
import com.app.builder.core.flow.Dispatcher
import com.app.builder.core.locale.now
import com.app.builder.core.telemetry.Telemetry

/**
 * Platform-agnostic device location capture controller intended to be extended by platform-specific implementations. Its update loop exists only to keep the platform's location cache warm.
 * Capture is foreground-only: the platform throttles or suspends background updates anyway, so [pauseUpdate] tears the loop down on a background transition and [resumeUpdate] rebuilds it.
 * Because the cache goes stale while the app is away, every start opens with a [Mode.Burst] that re-warms it as fast as the hardware allows before the loop settles into the low-power [Mode.Steady] cadence.
 */
open class DeviceLocationProvider {

    /** Observable state of location capture. */
    sealed interface State {
        /** No active capture. */
        data object Idle: State
        /** Actively capturing location fixes. */
        data object Active: State
    }

    /** Cadence the platform update loop is running at. */
    enum class Mode {
        /** High accuracy, unbatched and unfiltered, to re-warm the platform's location cache as fast as possible. Runs only until the first fresh fix lands or the burst times out. */
        Burst,
        /** Low-power cadence the loop settles into once the cache is warm. */
        Steady,
    }

    /** If the location provider is available on this device. */
    open val available: Boolean = false

    /** Backing state for [state]. */
    private val _state: MutableStateFlow<State> = MutableStateFlow(value = State.Idle)
    /** Observable state of location capture. */
    val state: StateFlow<State> = _state.asStateFlow()

    /** Backing state for [deviceLocation]. */
    private val _deviceLocation: MutableStateFlow<DeviceLocation?> = MutableStateFlow(value = null)
    /** The last location fix captured, or null if none has been captured yet. */
    val deviceLocation: StateFlow<DeviceLocation?> = _deviceLocation.asStateFlow()

    /** Scope driving the burst-to-steady transition. */
    private val scope: CoroutineScope = CoroutineScope(context = SupervisorJob())
    /** Tracks the currently open burst window. */
    private var burstJob: Job? = null
    /** The cadence the platform loop is currently running at. */
    private var mode: Mode = Mode.Steady
    /** Whether [resumeUpdate] should restart the loop, set by [pauseUpdate] and cleared by an explicit [stopUpdate]. */
    private var resumeOnForeground: Boolean = false

    /**
     * Returns if the location provider is permitted on this device.
     *
     * @return true if permitted, false otherwise.
     */
    open fun hasPermission(): Boolean = false

    /** Starts location updates, opening with a [Mode.Burst] before settling into [Mode.Steady]. Transitions to [State.Active]. */
    fun startUpdate() {
        runCatching {
            if (!ClientFlags.flags.locationCapture || !available || !hasPermission()) return stopUpdate()
            platformStopUpdate()
            startBurst()
            _state.update { State.Active }
        }.onFailure {
            Telemetry.error(tag = TAG, message = "Error starting capture", throwable = it)
            platformStopUpdate()
        }
    }

    /** Stops location updates for good. Transitions to [State.Idle] and stops [resumeUpdate] from restarting them. */
    fun stopUpdate() {
        resumeOnForeground = false
        stop()
    }

    /** Stops location updates for a background transition, remembering whether they were running so [resumeUpdate] can restart them. Transitions to [State.Idle]. */
    fun pauseUpdate() {
        val active = _state.value == State.Active
        stop()
        resumeOnForeground = active
    }

    /** Restarts location updates if [pauseUpdate] stopped them while they were running, re-warming the stale platform cache with a [Mode.Burst]. */
    fun resumeUpdate() {
        if (!resumeOnForeground) return
        startUpdate()
    }

    /**
     * Returns the last known location, preferring a fresh platform query and falling back to the last fix captured by the update loop if the platform can't provide one.
     *
     * @return The last known [DeviceLocation], or null if none is available.
     */
    suspend fun getLastKnownLocation(): DeviceLocation? = runCatching {
        if (!ClientFlags.flags.locationCapture || !available || !hasPermission()) {
            stopUpdate()
            return null
        }
        platformGetLastKnownLocation()
    }.onFailure {
        Telemetry.error(tag = TAG, message = "Error getting last known location", throwable = it)
    }.getOrNull()?.also { setLastKnownLocation(deviceLocation = it) } ?: _deviceLocation.value

    /**
     * Starts the platform location update at the given cadence.
     *
     * @param mode The cadence to run the platform loop at.
     */
    protected open fun platformStartUpdate(mode: Mode) {}

    /** Stops the platform location update. */
    protected open fun platformStopUpdate() {}

    /**
     * Queries the platform for its own cached last known location, if it supports one.
     *
     * @return The platform's last known [DeviceLocation], or null if unavailable.
     */
    protected open suspend fun platformGetLastKnownLocation(): DeviceLocation? = null

    /**
     * Sets the last [deviceLocation].
     *
     * @param deviceLocation The location fix that was captured.
     */
    protected fun setLastKnownLocation(deviceLocation: DeviceLocation) {
        _deviceLocation.update { deviceLocation }
    }

    /** Tears the platform loop down and transitions to [State.Idle], leaving [resumeOnForeground] untouched. */
    private fun stop() {
        burstJob?.cancel()
        burstJob = null
        mode = Mode.Steady
        runCatching {
            platformStopUpdate()
        }.onFailure {
            Telemetry.error(tag = TAG, message = "Error stopping capture", throwable = it)
        }
        _state.update { State.Idle }
    }

    /**
     * Starts the platform loop at the [Mode.Burst] cadence and settles it back to [Mode.Steady] as soon as a fix acquired after this call lands, or once the burst times out.
     * The fix is matched on [DeviceLocation.fixTime] rather than on a new emission, so a stale fix replayed out of the platform's cache by [getLastKnownLocation] can't end the burst early.
     */
    private fun startBurst() {
        burstJob?.cancel()
        val startedAt = now().toEpochMilliseconds()
        mode = Mode.Burst
        platformStartUpdate(mode = Mode.Burst)
        burstJob = scope.launch(context = Dispatcher.Main) {
            val warmed = withTimeoutOrNull(timeMillis = ClientConfigs.configs.locationBurstTimeoutMillis) {
                _deviceLocation.first { it != null && it.fixTime >= startedAt }
            } != null
            if (warmed) Telemetry.info(tag = TAG, message = "Cache re-warmed, settling capture to the steady cadence") else {
                Telemetry.error(tag = TAG, message = "Burst timed out without a fresh fix, settling capture to the steady cadence")
            }
            settle()
        }
    }

    /** Restarts the platform loop at the low-power [Mode.Steady] cadence. Only [startBurst] and [stop] own [burstJob], so this can be called from within it. */
    private fun settle() {
        if (mode == Mode.Steady || _state.value != State.Active) return
        runCatching {
            mode = Mode.Steady
            platformStopUpdate()
            platformStartUpdate(mode = Mode.Steady)
        }.onFailure {
            Telemetry.error(tag = TAG, message = "Error settling capture to the steady cadence", throwable = it)
            stopUpdate()
        }
    }

    companion object {
        private const val TAG = "DeviceLocation"

        /**
         * Creates a [DeviceLocationProvider].
         *
         * @return [DeviceLocationProvider] instance.
         */
        internal val instance: DeviceLocationProvider by lazy {
            runCatching {
                createDeviceLocationProvider().also { Telemetry.info(tag = TAG, message = "Device Location Provider created") }
            }.onFailure {
                Telemetry.error(tag = TAG, message = "Unable to create Device Location Provider and returning a no-op provider", throwable = it)
            }.getOrDefault(defaultValue = DeviceLocationProvider())
        }
    }
}

suspend fun getLastKnownLocation(): DeviceLocation? = DeviceLocationProvider.instance.getLastKnownLocation()

/**
 * Creates a platform-specific [DeviceLocationProvider] instance.
 *
 * @return A new [DeviceLocationProvider].
 */
internal expect fun createDeviceLocationProvider(): DeviceLocationProvider
