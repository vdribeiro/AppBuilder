package com.app.builder.core.devicelocation

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import com.app.builder.core.config.ClientFlags
import com.app.builder.core.telemetry.Telemetry

/** Platform-agnostic device location capture controller intended to be extended by platform-specific implementations. Its update loop exists only to keep the platform's location cache warm. */
open class DeviceLocationProvider {

    /** Observable state of location capture. */
    sealed interface State {
        /** No active capture. */
        data object Idle: State
        /** Actively capturing location fixes. */
        data object Active: State
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

    /**
     * Returns if the location provider is permitted on this device.
     *
     * @return true if permitted, false otherwise.
     */
    open fun hasPermission(): Boolean = false

    /** Starts location updates. Transitions to [State.Active]. */
    fun startUpdate() {
        runCatching {
            if (!ClientFlags.flags.locationCapture || !available || !hasPermission()) return stopUpdate()
            platformStopUpdate()
            platformStartUpdate()
            _state.update { State.Active }
        }.onFailure {
            Telemetry.error(tag = TAG, message = "Error starting capture", throwable = it)
            platformStopUpdate()
        }
    }

    /** Stops location updates. Transitions to [State.Idle]. */
    fun stopUpdate() {
        runCatching {
            platformStopUpdate()
        }.onFailure {
            Telemetry.error(tag = TAG, message = "Error stopping capture", throwable = it)
        }
        _state.update { State.Idle }
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

    /** Starts the platform location update. */
    protected open fun platformStartUpdate() {}

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
