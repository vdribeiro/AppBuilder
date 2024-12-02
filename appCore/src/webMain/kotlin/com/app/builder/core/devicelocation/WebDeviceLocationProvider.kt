package com.app.builder.core.devicelocation

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import com.app.builder.core.config.ClientConfigs
import com.app.builder.core.locale.now
import com.app.builder.core.platform.loop
import com.app.builder.core.security.uuid
import com.app.builder.core.telemetry.Telemetry
import com.app.builder.data.serializer.decode

/** Web [DeviceLocationProvider] implementation backed by the browser `navigator.geolocation` API. */
internal class WebDeviceLocationProvider: DeviceLocationProvider() {

    /** Scope used to poll for fixes delivered asynchronously by `watchPosition`. */
    private val scope = CoroutineScope(context = SupervisorJob())
    /** The active polling job, while capture is in progress. */
    private var pollJob: Job? = null

    override val available: Boolean = runCatching {
        isGeolocationSupported()
    }.onFailure {
        Telemetry.error(tag = TAG, message = "Unable to check audio player availability", throwable = it)
    }.getOrDefault(defaultValue = false)

    override fun hasPermission(): Boolean {
        super.hasPermission()
        return true
    }

    /**
     * Starts `navigator.geolocation.watchPosition` and polls the resulting queue for new fixes.
     * A burst asks the browser for a high-accuracy fix and drains the queue on the burst cadence, so a fix surfaces in the same order of time it takes the browser to produce one.
     *
     * @param mode The cadence to run the platform loop at.
     */
    override fun platformStartUpdate(mode: Mode) {
        super.platformStartUpdate(mode = mode)
        platformStopUpdate()
        startWatchingPosition(highAccuracy = mode == Mode.Burst)
        // The steady cadence drains on [DRAIN_INTERVAL_MILLIS] rather than on locationIntervalMillis, since `watchPosition` alone decides how often the browser pushes a fix and the queue has to be drained faster than it fills.
        val interval = when (mode) {
            Mode.Burst -> ClientConfigs.configs.locationBurstIntervalMillis
            Mode.Steady -> DRAIN_INTERVAL_MILLIS
        }
        pollJob = scope.launch {
            loop(timeMillis = interval) {
                if (pollWatchError()) return@loop platformStopUpdate()
                val raw = pollNextPosition()
                if (raw.isEmpty()) return@loop
                decode<WebPosition>(value = raw)?.let { setLastKnownLocation(deviceLocation = it.toDeviceLocation()) }
            }
        }
    }

    override fun platformStopUpdate() {
        super.platformStopUpdate()
        pollJob?.cancel()
        pollJob = null
        stopWatchingPosition()
    }

    /**
     * Converts a [WebPosition] to a [DeviceLocation].
     *
     * @receiver The browser geolocation fix to convert.
     * @return The equivalent [DeviceLocation].
     */
    private fun WebPosition.toDeviceLocation(): DeviceLocation = DeviceLocation(
        uuid = uuid(),
        provider = "browser",
        fixTime = timestamp,
        deviceTime = now().toEpochMilliseconds(),
        latitude = latitude,
        longitude = longitude,
        accuracy = accuracy,
        altitude = altitude,
        bearing = heading,
        speed = speed,
    )

    /** JSON payload pushed by [startWatchingPosition]'s success callback for one `GeolocationPosition`. */
    @Serializable
    private data class WebPosition(
        val latitude: Double,
        val longitude: Double,
        val accuracy: Double,
        val altitude: Double?,
        val heading: Double?,
        val speed: Double?,
        val timestamp: Long,
    )

    companion object {
        private const val TAG = "WebDeviceLocationProvider"

        /** Interval in milliseconds at which the `watchPosition` queue is drained outside of a burst. */
        private const val DRAIN_INTERVAL_MILLIS = 3_000L
    }
}

/** Checks whether this browser exposes `navigator.geolocation`. */
@JsFun(code = "() => 'geolocation' in navigator")
private external fun isGeolocationSupported(): Boolean

/**
 * Starts `navigator.geolocation.watchPosition`, clearing any previous watch first, and pushes each successful fix as a JSON string onto `window.__deviceLocationQueue` for [pollNextPosition] to drain.
 * A failed fix (e.g. denied/revoked permission) sets `window.__deviceLocationError` for [pollWatchError] to observe instead of being silently dropped.
 *
 * @param highAccuracy Whether to ask the browser for its most accurate fix, at the cost of power and latency to first fix.
 */
@JsFun(
    code = """(highAccuracy) => {
        window.__deviceLocationQueue = [];
        window.__deviceLocationError = false;
        if (window.__deviceLocationWatchId !== undefined && window.__deviceLocationWatchId !== null) {
            navigator.geolocation.clearWatch(window.__deviceLocationWatchId);
        }
        window.__deviceLocationWatchId = navigator.geolocation.watchPosition(
            function(position) {
                window.__deviceLocationQueue.push(JSON.stringify({
                    latitude: position.coords.latitude,
                    longitude: position.coords.longitude,
                    accuracy: position.coords.accuracy,
                    altitude: position.coords.altitude,
                    heading: position.coords.heading,
                    speed: position.coords.speed,
                    timestamp: position.timestamp
                }));
            },
            function() { window.__deviceLocationError = true; },
            { enableHighAccuracy: highAccuracy, maximumAge: 0 }
        );
    }"""
)
private external fun startWatchingPosition(highAccuracy: Boolean)

/** Clears the active `watchPosition` subscription and drops any pending fixes. */
@JsFun(
    code = """() => {
        if (window.__deviceLocationWatchId !== undefined && window.__deviceLocationWatchId !== null) {
            navigator.geolocation.clearWatch(window.__deviceLocationWatchId);
        }
        window.__deviceLocationWatchId = null;
        window.__deviceLocationQueue = [];
        window.__deviceLocationError = false;
    }"""
)
private external fun stopWatchingPosition()

/** Dequeues and returns the oldest pending fix pushed by [startWatchingPosition], or an empty string if none is available. */
@JsFun(code = "() => (window.__deviceLocationQueue && window.__deviceLocationQueue.length > 0) ? window.__deviceLocationQueue.shift() : ''")
private external fun pollNextPosition(): String

/** Returns and clears whether `watchPosition`'s error callback (e.g. permission denied/revoked) has fired since the last poll. */
@JsFun(code = "() => { var e = !!window.__deviceLocationError; window.__deviceLocationError = false; return e; }")
private external fun pollWatchError(): Boolean

internal actual fun createDeviceLocationProvider(): DeviceLocationProvider = WebDeviceLocationProvider()
