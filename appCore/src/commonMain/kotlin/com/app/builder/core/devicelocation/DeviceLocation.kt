package com.app.builder.core.devicelocation

import kotlin.uuid.Uuid

/**
 * A single device location fix reported by a device.
 *
 * @property uuid The unique, immutable identifier for this fix, generated on-device so retried deliveries stay idempotent.
 * @property provider The source of the fix, e.g. "gps", "network", "fused".
 * @property fixTime The Unix epoch time of this location fix recorded by the location provider in milliseconds since the start of the Unix epoch.
 * @property deviceTime The Unix epoch time of this location fix recorded by the device clock in milliseconds since the start of the Unix epoch.
 * @property latitude The latitude in degrees.
 * @property longitude The longitude in degrees.
 * @property altitude The altitude in meters above the WGS84 reference ellipsoid, if available.
 * @property accuracy The estimated radial accuracy in meters.
 * @property bearing The bearing in degrees measured clockwise from true north, if available.
 * @property speed The speed in meters/second over ground, if available.
 */
data class DeviceLocation(
    val uuid: Uuid,
    val provider: String,
    val fixTime: Long,
    val deviceTime: Long,
    val latitude: Double,
    val longitude: Double,
    val altitude: Double?,
    val accuracy: Double,
    val bearing: Double?,
    val speed: Double?,
)
