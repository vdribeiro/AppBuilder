package com.app.builder.data.http

import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import com.app.builder.applicationContext
import com.app.builder.core.telemetry.Telemetry

actual fun isInternetAvailable(): Boolean = runCatching {
    val connectivityManager = getConnectivityManager() ?: return false
    val activeNetwork = connectivityManager.activeNetwork ?: return false
    val capabilities = connectivityManager.getNetworkCapabilities(activeNetwork) ?: return false
    capabilities.isValidated()
}.onFailure {
    Telemetry.error(tag = TAG, message = "Unable to check internet connection", throwable = it)
}.getOrDefault(defaultValue = false)

actual fun getInternetAvailabilityFlow(): Flow<Boolean> = callbackFlow {
    trySend(element = isInternetAvailable())

    val connectivityManager = getConnectivityManager() ?: run {
        close()
        return@callbackFlow
    }

    val callback = object: ConnectivityManager.NetworkCallback() {
        override fun onLost(network: Network) {
            trySend(element = false)
        }

        override fun onCapabilitiesChanged(network: Network, networkCapabilities: NetworkCapabilities) {
            trySend(element = networkCapabilities.isValidated())
        }
    }

    runCatching {
        connectivityManager.registerDefaultNetworkCallback(callback)
    }.onFailure {
        Telemetry.error(tag = TAG, message = "Unable to register network callback", throwable = it)
        close()
        return@callbackFlow
    }

    awaitClose {
        runCatching {
            connectivityManager.unregisterNetworkCallback(callback)
        }.onFailure {
            Telemetry.error(tag = TAG, message = "Unable to unregister network callback", throwable = it)
        }
    }
}.distinctUntilChanged()

/**
 * Get the connectivity manager safely.
 *
 * @return The [ConnectivityManager], or null on error.
 */
private fun getConnectivityManager(): ConnectivityManager? = runCatching {
    applicationContext.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
}.onFailure {
    Telemetry.error(tag = TAG, message = "Unable to get connectivity manager", throwable = it)
}.getOrNull()

/**
 * Checks whether this [NetworkCapabilities] represents an internet-capable and validated connection.
 *
 * @receiver The [NetworkCapabilities] to check.
 * @return `true` if the network has internet capability and has been validated, `false` otherwise.
 */
private fun NetworkCapabilities.isValidated(): Boolean =
    hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) && hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)

private const val TAG = "Network"