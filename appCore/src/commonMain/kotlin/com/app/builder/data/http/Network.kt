package com.app.builder.data.http

import kotlinx.coroutines.flow.Flow

/**
 * Checks if an active internet connection is currently available on the device.
 *
 * @return `true` if the device is connected to the internet, `false` otherwise.
 */
expect fun isInternetAvailable(): Boolean

/**
 * Returns a reactive stream that emits the current internet availability status.
 * Emits `true` when the connection is restored, and `false` when it is lost.
 *
 * @return A [Flow] emitting the network availability state.
 */
expect fun getInternetAvailabilityFlow(): Flow<Boolean>
