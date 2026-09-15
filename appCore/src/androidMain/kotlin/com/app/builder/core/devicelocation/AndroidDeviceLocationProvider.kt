package com.app.builder.core.devicelocation

import kotlinx.coroutines.suspendCancellableCoroutine
import android.Manifest
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Looper
import androidx.annotation.RequiresPermission
import androidx.core.content.ContextCompat
import androidx.core.location.LocationManagerCompat
import com.app.builder.applicationContext
import com.app.builder.core.config.ClientConfigs
import com.app.builder.core.config.ClientFlags
import com.app.builder.core.locale.now
import com.app.builder.core.security.uuid
import com.app.builder.core.telemetry.Telemetry
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.Task

/** Android [DeviceLocationProvider] implementation backed by the Fused Location Provider, falling back to the platform [LocationManager] when Google Play Services is unavailable. */
internal class AndroidDeviceLocationProvider: DeviceLocationProvider() {

    /** The platform location manager, used for availability checks and as the fallback source when Play Services is unavailable. */
    private val locationManager: LocationManager? = runCatching {
        applicationContext.getSystemService(LocationManager::class.java)
    }.onFailure {
        Telemetry.error(tag = TAG, message = "Unable to get the Location Manager", throwable = it)
    }.getOrNull()
    /** The Fused Location Provider client, or null if Google Play Services is unavailable on this device. */
    private val fusedClient: FusedLocationProviderClient? = runCatching {
        if (isPlayServicesAvailable()) LocationServices.getFusedLocationProviderClient(applicationContext) else null
    }.onFailure {
        Telemetry.error(tag = TAG, message = "Unable to get the Fused Location Provider", throwable = it)
    }.getOrNull()

    /** The active Fused Location Provider callback, while capturing through [fusedClient]. */
    private var fusedCallback: LocationCallback? = null
    /** The active platform [LocationManager] listener, while capturing through the [locationManager] fallback. */
    private var platformListener: LocationListener? = null

    override val available: Boolean = runCatching {
        locationManager?.let { LocationManagerCompat.isLocationEnabled(it) } ?: false
    }.onFailure {
        Telemetry.error(tag = TAG, message = "Unable to check audio player availability", throwable = it)
    }.getOrDefault(defaultValue = false)

    /**
     * Checks whether either the fine or coarse location permission is currently granted.
     *
     * @return True if at least one location permission is granted.
     */
    override fun hasPermission(): Boolean {
        super.hasPermission()
        return runCatching {
            ContextCompat.checkSelfPermission(applicationContext, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
                    ContextCompat.checkSelfPermission(applicationContext, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        }.onFailure {
            Telemetry.error(tag = TAG, message = "Unable to check location provider permission", throwable = it)
        }.getOrDefault(defaultValue = false)
    }

    @RequiresPermission(anyOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION])
    override fun platformStartUpdate() {
        super.platformStartUpdate()
        when {
            fusedClient != null -> startFusedUpdates(fusedClient = fusedClient)
            locationManager != null -> startPlatformUpdates(locationManager = locationManager)
        }
    }

    override fun platformStopUpdate() {
        super.platformStopUpdate()
        fusedCallback?.let { fusedClient?.removeLocationUpdates(it) }
        fusedCallback = null
        platformListener?.let { locationManager?.removeUpdates(it) }
        platformListener = null
    }

    @RequiresPermission(anyOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION])
    override suspend fun platformGetLastKnownLocation(): DeviceLocation? {
        super.platformGetLastKnownLocation()
        val location = fusedClient?.lastKnownLocation() ?: locationManager?.lastKnownLocation()
        return location?.toDeviceLocation()
    }

    /**
     * Checks whether Google Play Services is available on this device.
     *
     * @return True if the Fused Location Provider can be used.
     */
    private fun isPlayServicesAvailable(): Boolean =
        GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(applicationContext) == ConnectionResult.SUCCESS

    /**
     * Starts continuous updates through the Fused Location Provider.
     *
     * @param fusedClient The Fused Location Provider client.
     */
    @RequiresPermission(anyOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION])
    private fun startFusedUpdates(fusedClient: FusedLocationProviderClient) {
        // locationIntervalMillis is the fastest tier; the desired and max-wait (batching) tiers are derived from it as fixed multiples
        // rather than exposed as separate remote configs, since only Android's request model distinguishes between the three.
        val interval = ClientConfigs.configs.locationIntervalMillis
        val request = LocationRequest.Builder(interval * 2)
            .setMinUpdateIntervalMillis(interval)
            .setMaxUpdateDelayMillis(interval * 4)
            .setMinUpdateDistanceMeters(ClientConfigs.configs.locationDisplacementMeters)
            .setPriority(Priority.PRIORITY_BALANCED_POWER_ACCURACY)
            .build()
        val callback = object: LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                if (!ClientFlags.flags.locationCapture || !available || !hasPermission()) return platformStopUpdate()
                result.locations.lastOrNull()?.let { setLastKnownLocation(deviceLocation = it.toDeviceLocation()) }
            }
        }
        fusedCallback = callback
        fusedClient.requestLocationUpdates(
            request,
            callback,
            Looper.getMainLooper()
        )
    }

    /**
     * Starts continuous updates through the platform [LocationManager].
     *
     * @param locationManager The platform location manager.
     */
    @RequiresPermission(anyOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION])
    private fun startPlatformUpdates(locationManager: LocationManager) {
        val provider = when {
            locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) -> LocationManager.GPS_PROVIDER
            locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER) -> LocationManager.NETWORK_PROVIDER
            else -> return platformStopUpdate()
        }
        val listener = LocationListener { location ->
            if (!ClientFlags.flags.locationCapture || !available || !hasPermission()) return@LocationListener platformStopUpdate()
            setLastKnownLocation(deviceLocation = location.toDeviceLocation())
        }
        platformListener = listener
        locationManager.requestLocationUpdates(
            provider,
            ClientConfigs.configs.locationIntervalMillis,
            ClientConfigs.configs.locationDisplacementMeters,
            listener,
            Looper.getMainLooper()
        )
    }

    /**
     * Awaits this Play Services [Task], resolving to null instead of throwing on failure.
     *
     * @receiver The task to await.
     * @return The task's result, or null if it failed or produced none.
     */
    private suspend fun Task<Location>.await(): Location? = suspendCancellableCoroutine { continuation ->
        addOnSuccessListener { continuation.resumeWith(result = Result.success(value = it)) }
        addOnFailureListener { continuation.resumeWith(result = Result.success(value = null)) }
    }

    /**
     * Returns the freshest cached location reported by the [FusedLocationProviderClient].
     *
     * @receiver The fused location provider to query.
     * @return The freshest cached [Location], or null if none is available.
     */
    @RequiresPermission(anyOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION])
    private suspend fun FusedLocationProviderClient.lastKnownLocation(): Location? = runCatching { lastLocation.await() }.getOrNull()

    /**
     * Returns the freshest cached location reported by any enabled provider on [LocationManager].
     *
     * @receiver The platform location manager to query.
     * @return The freshest cached [Location], or null if none is available.
     */
    @RequiresPermission(anyOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION])
    private fun LocationManager.lastKnownLocation(): Location? = listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER)
        .filter { isProviderEnabled(it) }
        .mapNotNull { runCatching { getLastKnownLocation(it) }.getOrNull() }
        .maxByOrNull { it.time }

    /**
     * Converts a platform [Location] to a [DeviceLocation].
     *
     * @receiver The platform location to convert.
     * @return The equivalent [DeviceLocation].
     */
    private fun Location.toDeviceLocation(): DeviceLocation = DeviceLocation(
        uuid = uuid(),
        provider = provider ?: "unknown",
        fixTime = time,
        deviceTime = now().toEpochMilliseconds(),
        latitude = latitude,
        longitude = longitude,
        accuracy = if (hasAccuracy()) accuracy.toDouble() else Float.MAX_VALUE.toDouble(),
        altitude = if (hasAltitude()) altitude else null,
        bearing = if (hasBearing()) bearing.toDouble() else null,
        speed = if (hasSpeed()) speed.toDouble() else null,
    )

    companion object {
        private const val TAG = "AndroidDeviceLocationProvider"
    }
}

internal actual fun createDeviceLocationProvider(): DeviceLocationProvider = AndroidDeviceLocationProvider()
