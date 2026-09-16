package com.app.builder.core.devicelocation

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
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

    /** Scope used to poll the browser for fixes. */
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
        // The browser prompts for access at the point of use
        return true
    }

    /** Polls `navigator.geolocation.getCurrentPosition` on [ClientConfigs.locationIntervalMillis]. */
    override fun platformStartUpdate() {
        super.platformStartUpdate()
        platformStopUpdate()
        pollJob = scope.launch {
            loop(timeMillis = ClientConfigs.configs.locationIntervalMillis) {
                position(
                    slot = CAPTURE_SLOT,
                    browserTimeoutMillis = ClientConfigs.configs.locationQueryTimeoutMillis.toInt(),
                    maximumAgeMillis = ClientConfigs.configs.locationIntervalMillis.toInt(),
                )?.let { setLastKnownLocation(deviceLocation = it) }
            }
        }
    }

    override fun platformStopUpdate() {
        super.platformStopUpdate()
        pollJob?.cancel()
        pollJob = null
        clearPosition(slot = CAPTURE_SLOT)
    }

    /**
     * Returns a position the browser already holds, without letting it acquire a new one.
     * `maximumAge` is effectively unbounded and the browser's own `timeout` is zero, so this reads what is already known rather than powering anything on, matching what the other platforms expose as a cached last known location.
     * It runs on its own slot so that a read landing while the capture loop has a request in flight cannot collect the loop's fix, or have the loop collect this one.
     *
     * @return The browser's cached [DeviceLocation], or null if it holds none.
     */
    override suspend fun platformGetLastKnownLocation(): DeviceLocation? {
        super.platformGetLastKnownLocation()
        return position(slot = READ_SLOT, browserTimeoutMillis = 0, maximumAgeMillis = Int.MAX_VALUE)
    }

    /**
     * Issues one `getCurrentPosition` against [slot] and waits for the browser to settle it, since the fix arrives on a callback rather than being returned.
     * The wait is bounded by [ClientConfigs.locationQueryTimeoutMillis] independently of the browser's own timeout, so a browser that never calls either callback cannot hold the caller.
     *
     * @param slot The window property the request stores its outcome on.
     * @param browserTimeoutMillis How long the browser may spend acquiring before reporting failure. Zero forbids acquisition, leaving only what is already cached.
     * @param maximumAgeMillis How old a position already cached by the browser may be for it to answer with that instead of acquiring a new one.
     * @return The [DeviceLocation] the browser reported, or null if it reported failure or never settled.
     */
    private suspend fun position(slot: String, browserTimeoutMillis: Int, maximumAgeMillis: Int): DeviceLocation? {
        requestPosition(slot = slot, highAccuracy = false, timeoutMillis = browserTimeoutMillis, maximumAgeMillis = maximumAgeMillis)
        var waited = 0L
        val timeout = ClientConfigs.configs.locationQueryTimeoutMillis
        while (waited <= timeout) {
            if (pollPositionError(slot = slot)) return null
            val raw = pollPosition(slot = slot)
            if (raw.isNotEmpty()) return decode<WebPosition>(value = raw)?.toDeviceLocation()
            delay(timeMillis = SETTLE_POLL_MILLIS)
            waited += SETTLE_POLL_MILLIS
        }
        return null
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

    /** JSON payload stored by [requestPosition]'s success callback for one `GeolocationPosition`. */
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

        /** Interval in milliseconds at which the browser's callback slot is checked while a request is in flight. */
        private const val SETTLE_POLL_MILLIS = 100L

        /** Window property the capture loop stores its outcome on. */
        private const val CAPTURE_SLOT = "__deviceLocationCapture"

        /** Window property an on-demand read stores its outcome on, kept apart from [CAPTURE_SLOT] so the two cannot collect each other's fixes. */
        private const val READ_SLOT = "__deviceLocationRead"
    }
}

/** Checks whether this browser exposes `navigator.geolocation`. */
@JsFun(code = "() => 'geolocation' in navigator")
private external fun isGeolocationSupported(): Boolean

/**
 * Clears [slot] and issues one `navigator.geolocation.getCurrentPosition`, storing the fix as a JSON string on that window property for [pollPosition] to collect.
 * A failed fix sets the matching error property for [pollPositionError] to observe.
 *
 * @param slot The window property to store the outcome on.
 * @param highAccuracy Whether to ask the browser for its most accurate fix, at the cost of power and latency to first fix.
 * @param timeoutMillis How long the browser may spend acquiring before reporting failure.
 * @param maximumAgeMillis How old a position already cached by the browser may be for it to answer with that instead of acquiring a new one.
 */
@JsFun(
    code = """(slot, highAccuracy, timeoutMillis, maximumAgeMillis) => {
        window[slot] = '';
        window[slot + 'Error'] = false;
        navigator.geolocation.getCurrentPosition(
            function(position) {
                window[slot] = JSON.stringify({
                    latitude: position.coords.latitude,
                    longitude: position.coords.longitude,
                    accuracy: position.coords.accuracy,
                    altitude: position.coords.altitude,
                    heading: position.coords.heading,
                    speed: position.coords.speed,
                    timestamp: position.timestamp
                });
            },
            function() { window[slot + 'Error'] = true; },
            { enableHighAccuracy: highAccuracy, timeout: timeoutMillis, maximumAge: maximumAgeMillis }
        );
    }"""
)
private external fun requestPosition(slot: String, highAccuracy: Boolean, timeoutMillis: Int, maximumAgeMillis: Int)

/**
 * Returns and clears the fix stored on [slot], or an empty string if none has arrived yet.
 *
 * @param slot The window property to read.
 */
@JsFun(code = "(slot) => { var r = window[slot] || ''; window[slot] = ''; return r; }")
private external fun pollPosition(slot: String): String

/**
 * Returns and clears whether the request on [slot] reported failure since the last poll.
 *
 * @param slot The window property to read.
 */
@JsFun(code = "(slot) => { var e = !!window[slot + 'Error']; window[slot + 'Error'] = false; return e; }")
private external fun pollPositionError(slot: String): Boolean

/**
 * Discards any fix or error left on [slot] by a request that is no longer wanted.
 *
 * @param slot The window property to clear.
 */
@JsFun(code = "(slot) => { window[slot] = ''; window[slot + 'Error'] = false; }")
private external fun clearPosition(slot: String)

internal actual fun createDeviceLocationProvider(): DeviceLocationProvider = WebDeviceLocationProvider()
