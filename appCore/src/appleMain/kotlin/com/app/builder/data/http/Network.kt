package com.app.builder.data.http

import kotlinx.cinterop.CValuesRef
import kotlinx.cinterop.MemScope
import kotlinx.cinterop.alloc
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.reinterpret
import kotlinx.cinterop.sizeOf
import kotlinx.cinterop.value
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import platform.CoreFoundation.CFRelease
import platform.CoreFoundation.CFTypeRef
import platform.Network.nw_path_get_status
import platform.Network.nw_path_monitor_cancel
import platform.Network.nw_path_monitor_create
import platform.Network.nw_path_monitor_set_queue
import platform.Network.nw_path_monitor_set_update_handler
import platform.Network.nw_path_monitor_start
import platform.Network.nw_path_status_satisfied
import platform.SystemConfiguration.SCNetworkReachabilityCreateWithAddress
import platform.SystemConfiguration.SCNetworkReachabilityFlagsVar
import platform.SystemConfiguration.SCNetworkReachabilityGetFlags
import platform.SystemConfiguration.kSCNetworkReachabilityFlagsConnectionRequired
import platform.SystemConfiguration.kSCNetworkReachabilityFlagsReachable
import platform.darwin.dispatch_get_main_queue
import platform.posix.AF_INET
import platform.posix.AF_INET6
import platform.posix.sockaddr
import platform.posix.sockaddr_in
import platform.posix.sockaddr_in6
import com.app.builder.core.telemetry.Telemetry

actual fun isInternetAvailable(): Boolean = runCatching {
    memScoped {
        checkAddress(address = alloc<sockaddr_in>().apply {
            sin_len = sizeOf<sockaddr_in>().toUByte()
            sin_family = AF_INET.toUByte()
        }.ptr.reinterpret()) || checkAddress(address = alloc<sockaddr_in6>().apply {
            sin6_len = sizeOf<sockaddr_in6>().toUByte()
            sin6_family = AF_INET6.toUByte()
        }.ptr.reinterpret())
    }
}.onFailure {
    Telemetry.error(tag = TAG, message = "Unable to check internet connection", throwable = it)
}.getOrDefault(defaultValue = false)

actual fun getInternetAvailabilityFlow(): Flow<Boolean> = callbackFlow {
    trySend(element = isInternetAvailable())

    val monitor = nw_path_monitor_create()
    if (monitor == null) {
        Telemetry.error(tag = TAG, message = "Failed to create nw_path_monitor")
        close()
        return@callbackFlow
    }

    nw_path_monitor_set_update_handler(monitor = monitor) { path ->
        trySend(element = nw_path_get_status(path = path) == nw_path_status_satisfied)
    }

    nw_path_monitor_set_queue(monitor = monitor, queue = dispatch_get_main_queue())
    nw_path_monitor_start(monitor = monitor)

    awaitClose { nw_path_monitor_cancel(monitor = monitor) }
}.distinctUntilChanged()

/**
 * Checks reachability for the given socket [address] using SystemConfiguration.
 *
 * @receiver The [MemScope] used to allocate native memory for the reachability check.
 * @param address The socket address to test for reachability.
 * @return `true` if the address is reachable without requiring an additional connection, `false` otherwise.
 */
private fun MemScope.checkAddress(address: CValuesRef<sockaddr>): Boolean =
    SCNetworkReachabilityCreateWithAddress(
        allocator = null,
        address = address
    )?.use {
        val networkStatus: SCNetworkReachabilityFlagsVar = alloc<SCNetworkReachabilityFlagsVar>()
        val success = SCNetworkReachabilityGetFlags(target = it, flags = networkStatus.ptr)
        if (!success) return false
        val isReachable = (networkStatus.value and kSCNetworkReachabilityFlagsReachable) != 0u
        val needsConnection = (networkStatus.value and kSCNetworkReachabilityFlagsConnectionRequired) != 0u
        isReachable && !needsConnection
    } ?: false

/**
 * Runs [block] with this Core Foundation reference and releases it afterward via [CFRelease].
 *
 * @receiver The Core Foundation reference to release after use.
 * @param block The operation to perform with the reference.
 * @return The result of [block].
 */
private inline fun <T: CFTypeRef?, R> T.use(block: (T) -> R): R {
    try {
        return block(this)
    } finally {
        if (this != null) CFRelease(cf = this)
    }
}

private const val TAG = "Network"