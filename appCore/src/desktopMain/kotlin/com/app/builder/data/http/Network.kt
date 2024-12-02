package com.app.builder.data.http

import java.net.NetworkInterface
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flow
import com.app.builder.core.platform.loop
import com.app.builder.core.telemetry.Telemetry

actual fun isInternetAvailable(): Boolean = runCatching {
    NetworkInterface.getNetworkInterfaces().asSequence().any { it.isUp && !it.isLoopback }
}.onFailure {
    Telemetry.error(tag = TAG, message = "Unable to check internet connection", throwable = it)
}.getOrDefault(defaultValue = false)

actual fun getInternetAvailabilityFlow(): Flow<Boolean> = flow {
    loop { emit(value = isInternetAvailable()) }
}.distinctUntilChanged()

private const val TAG = "Network"
