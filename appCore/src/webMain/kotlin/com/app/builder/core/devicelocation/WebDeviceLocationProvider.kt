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
                requestPosition(
                    highAccuracy = false,
                    timeoutMillis = ClientConfigs.configs.locationQueryTimeoutMillis.toInt(),
                    maximumAgeMillis = ClientConfigs.configs.locationIntervalMillis.toInt(),
                )
                awaitPosition()
            }
        }
    }

    override fun platformStopUpdate() {
        super.platformStopUpdate()
        pollJob?.cancel()
        pollJob = null
        clearPosition()
    }

    /** Waits for the in-flight [requestPosition] to settle and captures the fix if one arrives. */
    private suspend fun awaitPosition() {
        var waited = 0L
        val timeout = ClientConfigs.configs.locationQueryTimeoutMillis
        while (waited <= timeout) {
            if (pollPositionError()) return
            val raw = pollPosition()
            if (raw.isNotEmpty()) {
                decode<WebPosition>(value = raw)?.let { setLastKnownLocation(deviceLocation = it.toDeviceLocation()) }
                return
            }
            delay(timeMillis = SETTLE_POLL_MILLIS)
            waited += SETTLE_POLL_MILLIS
        }
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
    }
}

/** Checks whether this browser exposes `navigator.geolocation`. */
@JsFun(code = "() => 'geolocation' in navigator")
private external fun isGeolocationSupported(): Boolean

/**
 * Clears any previous outcome and issues one `navigator.geolocation.getCurrentPosition`, storing the fix as a JSON string on `window.__deviceLocationResult` for [pollPosition] to collect.
 * A failed fix sets `window.__deviceLocationError` for [pollPositionError] to observe.
 *
 * @param highAccuracy Whether to ask the browser for its most accurate fix, at the cost of power and latency to first fix.
 * @param timeoutMillis How long the browser may spend acquiring before reporting failure.
 * @param maximumAgeMillis How old a position already cached by the browser may be for it to answer with that instead of acquiring a new one.
 */
@JsFun(
    code = """(highAccuracy, timeoutMillis, maximumAgeMillis) => {
        window.__deviceLocationResult = '';
        window.__deviceLocationError = false;
        navigator.geolocation.getCurrentPosition(
            function(position) {
                window.__deviceLocationResult = JSON.stringify({
                    latitude: position.coords.latitude,
                    longitude: position.coords.longitude,
                    accuracy: position.coords.accuracy,
                    altitude: position.coords.altitude,
                    heading: position.coords.heading,
                    speed: position.coords.speed,
                    timestamp: position.timestamp
                });
            },
            function() { window.__deviceLocationError = true; },
            { enableHighAccuracy: highAccuracy, timeout: timeoutMillis, maximumAge: maximumAgeMillis }
        );
    }"""
)
private external fun requestPosition(highAccuracy: Boolean, timeoutMillis: Int, maximumAgeMillis: Int)

/** Returns and clears the fix stored by [requestPosition], or an empty string if none has arrived yet. */
@JsFun(code = "() => { var r = window.__deviceLocationResult || ''; window.__deviceLocationResult = ''; return r; }")
private external fun pollPosition(): String

/** Returns and clears whether [requestPosition]'s error callback has fired since the last poll. */
@JsFun(code = "() => { var e = !!window.__deviceLocationError; window.__deviceLocationError = false; return e; }")
private external fun pollPositionError(): Boolean

/** Discards any fix or error left behind by a request that is no longer wanted. */
@JsFun(code = "() => { window.__deviceLocationResult = ''; window.__deviceLocationError = false; }")
private external fun clearPosition()

internal actual fun createDeviceLocationProvider(): DeviceLocationProvider = WebDeviceLocationProvider()
