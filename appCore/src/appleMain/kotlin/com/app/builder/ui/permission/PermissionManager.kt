package com.app.builder.ui.permission

import kotlin.coroutines.resume
import kotlinx.coroutines.suspendCancellableCoroutine
import platform.AVFoundation.AVAuthorizationStatusAuthorized
import platform.AVFoundation.AVAuthorizationStatusNotDetermined
import platform.AVFoundation.AVCaptureDevice
import platform.AVFoundation.AVMediaTypeVideo
import platform.AVFoundation.authorizationStatusForMediaType
import platform.AVFoundation.requestAccessForMediaType
import platform.CoreLocation.CLLocationManager
import platform.CoreLocation.CLLocationManagerDelegateProtocol
import platform.CoreLocation.kCLAuthorizationStatusAuthorizedAlways
import platform.CoreLocation.kCLAuthorizationStatusAuthorizedWhenInUse
import platform.CoreLocation.kCLAuthorizationStatusDenied
import platform.CoreLocation.kCLAuthorizationStatusRestricted
import platform.UserNotifications.UNAuthorizationOptionAlert
import platform.UserNotifications.UNAuthorizationOptionBadge
import platform.UserNotifications.UNAuthorizationOptionSound
import platform.UserNotifications.UNAuthorizationStatusAuthorized
import platform.UserNotifications.UNUserNotificationCenter
import platform.darwin.NSObject
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

/** Apple [PermissionManager] implementation. */
private class ApplePermissionManager: PermissionManager() {

    /** The active [CLLocationManager] instance while a location permission request is in flight. */
    private var locationManager: CLLocationManager? = null
    /** The active [LocationPermissionDelegate] instance while a location permission request is in flight. */
    private var locationDelegate: LocationPermissionDelegate? = null

    override suspend fun hasPermission(permission: Permission): Boolean {
        super.hasPermission(permission = permission)
        return when (permission) {
            Permission.NOTIFICATIONS -> suspendCancellableCoroutine { continuation ->
                UNUserNotificationCenter.currentNotificationCenter().getNotificationSettingsWithCompletionHandler { settings ->
                    continuation.resume(value = settings?.authorizationStatus == UNAuthorizationStatusAuthorized)
                }
            }

            Permission.CAMERA -> {
                val status = AVCaptureDevice.authorizationStatusForMediaType(mediaType = AVMediaTypeVideo)
                status == AVAuthorizationStatusAuthorized
            }

            Permission.LOCATION -> {
                val status = CLLocationManager().authorizationStatus
                status == kCLAuthorizationStatusAuthorizedWhenInUse || status == kCLAuthorizationStatusAuthorizedAlways
            }
        }
    }

    override suspend fun requestPermission(permission: Permission): Boolean {
        super.requestPermission(permission = permission)
        return when (permission) {
            Permission.NOTIFICATIONS -> suspendCancellableCoroutine { continuation ->
                UNUserNotificationCenter.currentNotificationCenter().requestAuthorizationWithOptions(
                    options = UNAuthorizationOptionAlert or UNAuthorizationOptionSound or UNAuthorizationOptionBadge
                ) { granted, _ -> continuation.resume(value = granted) }
            }

            Permission.CAMERA -> when (AVCaptureDevice.authorizationStatusForMediaType(mediaType = AVMediaTypeVideo)) {
                AVAuthorizationStatusAuthorized -> true
                AVAuthorizationStatusNotDetermined -> suspendCancellableCoroutine { continuation ->
                    AVCaptureDevice.requestAccessForMediaType(mediaType = AVMediaTypeVideo) { continuation.resume(value = it) }
                }

                else -> false
            }

            Permission.LOCATION -> when (CLLocationManager().authorizationStatus) {
                kCLAuthorizationStatusDenied, kCLAuthorizationStatusRestricted -> false
                else -> suspendCancellableCoroutine { continuation ->
                    val manager = CLLocationManager()
                    val delegate = LocationPermissionDelegate { granted ->
                        if (continuation.isActive) continuation.resume(value = granted)
                        manager.delegate = null
                        locationManager = null
                        locationDelegate = null
                    }
                    manager.delegate = delegate
                    locationManager = manager
                    locationDelegate = delegate
                    continuation.invokeOnCancellation {
                        manager.delegate = null
                        locationManager = null
                        locationDelegate = null
                    }
                    manager.requestWhenInUseAuthorization()
                }
            }
        }
    }
}

/**
 * CoreLocation delegate that forwards the outcome of an authorization change to [onResult].
 *
 * @property onResult Invoked with `true` if location access was granted, `false` otherwise.
 */
private class LocationPermissionDelegate(
    private val onResult: (Boolean) -> Unit
): NSObject(), CLLocationManagerDelegateProtocol {

    override fun locationManagerDidChangeAuthorization(manager: CLLocationManager) {
        when (manager.authorizationStatus) {
            kCLAuthorizationStatusAuthorizedWhenInUse,
            kCLAuthorizationStatusAuthorizedAlways -> onResult(true)

            kCLAuthorizationStatusDenied,
            kCLAuthorizationStatusRestricted -> onResult(false)

            else -> Unit
        }
    }
}

@Composable
actual fun rememberPermissionManager(): PermissionManager = remember { ApplePermissionManager() }
