package com.app.builder.core.devicelocation

import kotlinx.cinterop.useContents
import platform.CoreLocation.CLLocation
import platform.CoreLocation.CLLocationManager
import platform.CoreLocation.CLLocationManagerDelegateProtocol
import platform.CoreLocation.kCLAuthorizationStatusAuthorizedAlways
import platform.CoreLocation.kCLAuthorizationStatusAuthorizedWhenInUse
import platform.CoreLocation.kCLLocationAccuracyHundredMeters
import platform.Foundation.NSError
import platform.Foundation.NSTimer
import platform.Foundation.timeIntervalSince1970
import platform.darwin.NSObject
import com.app.builder.core.config.ClientConfigs
import com.app.builder.core.locale.now
import com.app.builder.core.security.uuid
import com.app.builder.core.telemetry.Telemetry

/** Apple [DeviceLocationProvider] implementation backed by [CLLocationManager]. */
internal class AppleDeviceLocationProvider: DeviceLocationProvider() {

    /** The active location manager, while capture is in progress. */
    private var manager: CLLocationManager? = null
    /** The delegate backing [manager], kept alive for the manager's lifetime since [CLLocationManager.delegate] is a weak reference. */
    private var delegate: LocationDelegate? = null
    /** The repeating timer driving [CLLocationManager.requestLocation] polls, while capture is in progress. */
    private var timer: NSTimer? = null

    override val available: Boolean = runCatching {
        CLLocationManager.locationServicesEnabled()
    }.onFailure {
        Telemetry.error(tag = TAG, message = "Unable to check location provider availability", throwable = it)
    }.getOrDefault(defaultValue = false)

    /**
     * Checks whether location access is currently authorized.
     *
     * @return true if the app is authorized to receive location updates, false otherwise.
     */
    override fun hasPermission(): Boolean {
        super.hasPermission()
        return runCatching {
            val status = CLLocationManager().authorizationStatus
            return status == kCLAuthorizationStatusAuthorizedWhenInUse || status == kCLAuthorizationStatusAuthorizedAlways
        }.onFailure {
            Telemetry.error(tag = TAG, message = "Unable to check location access permission", throwable = it)
        }.getOrDefault(defaultValue = false)
    }

    override fun platformStartUpdate() {
        super.platformStartUpdate()
        val newDelegate = LocationDelegate(
            onLocations = {
                val location = it.lastOrNull() ?: return@LocationDelegate
                setLastKnownLocation(deviceLocation = location.toDeviceLocation())
            },
            onError = {
                Telemetry.error(tag = TAG, message = "Location capture failed", throwable = Throwable(message = it.localizedDescription))
                platformStopUpdate()
            },
            onAuthorizationChange = {
                if (!hasPermission()) {
                    Telemetry.error(tag = TAG, message = "Permission denied", throwable = Throwable(message = "Missing location authorization"))
                    platformStopUpdate()
                }
            },
        )
        val newManager = CLLocationManager().apply {
            delegate = newDelegate
            desiredAccuracy = kCLLocationAccuracyHundredMeters
        }
        manager = newManager
        delegate = newDelegate

        newManager.requestLocation()
        timer = NSTimer.scheduledTimerWithTimeInterval(
            interval = ClientConfigs.configs.locationIntervalMillis / 1000.0,
            repeats = true
        ) { manager?.requestLocation() }
    }

    override fun platformStopUpdate() {
        super.platformStopUpdate()
        timer?.invalidate()
        timer = null
        manager?.stopUpdatingLocation()
        manager = null
        delegate = null
    }

    override suspend fun platformGetLastKnownLocation(): DeviceLocation? {
        super.platformGetLastKnownLocation()
        return CLLocationManager().location?.toDeviceLocation()
    }

    /**
     * Core Location manager delegate.
     *
     * @property onLocations Called with each batch of locations whenever Core Location delivers an update.
     * @property onError Called when Core Location reports a capture failure.
     * @property onAuthorizationChange Called whenever the location authorization status changes, including a mid-session revocation.
     */
    private class LocationDelegate(
        private val onLocations: (List<CLLocation>) -> Unit,
        private val onError: (NSError) -> Unit,
        private val onAuthorizationChange: () -> Unit,
    ): NSObject(), CLLocationManagerDelegateProtocol {

        override fun locationManager(manager: CLLocationManager, didUpdateLocations: List<*>) {
            @Suppress("UNCHECKED_CAST")
            onLocations(didUpdateLocations as List<CLLocation>)
        }

        override fun locationManager(manager: CLLocationManager, didFailWithError: NSError) {
            onError(didFailWithError)
        }

        override fun locationManagerDidChangeAuthorization(manager: CLLocationManager) {
            onAuthorizationChange()
        }
    }

    /**
     * Converts a Core Location [CLLocation] to a [DeviceLocation].
     *
     * @receiver The Core Location fix to convert.
     * @return The equivalent [DeviceLocation].
     */
    private fun CLLocation.toDeviceLocation(): DeviceLocation = DeviceLocation(
        uuid = uuid(),
        provider = "fused",
        fixTime = (timestamp.timeIntervalSince1970 * 1000).toLong(),
        deviceTime = now().toEpochMilliseconds(),
        latitude = coordinate.useContents { latitude },
        longitude = coordinate.useContents { longitude },
        accuracy = horizontalAccuracy,
        altitude = if (verticalAccuracy >= 0) altitude else null,
        bearing = if (course >= 0) course else null,
        speed = if (speed >= 0) speed else null,
    )

    companion object {
        private const val TAG = "AppleDeviceLocationProvider"
    }
}

internal actual fun createDeviceLocationProvider(): DeviceLocationProvider = AppleDeviceLocationProvider()
