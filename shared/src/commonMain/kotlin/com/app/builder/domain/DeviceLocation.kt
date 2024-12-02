package com.app.builder.domain

import kotlin.time.Instant
import kotlin.uuid.Uuid
import kotlinx.serialization.Serializable

/**
 * A single device location fix reported by a device.
 *
 * @property uuid The unique, immutable identifier for this fix, generated on-device so retried deliveries stay idempotent.
 * @property userUuid The unique identifier of the user.
 * @property provider The source of the fix, e.g. "gps", "network", "fused".
 * @property fixTime The UTC of this location fix recorded by the location provider.
 * @property deviceTime The UTC of this location fix recorded by the device clock.
 * @property latitude The latitude in degrees.
 * @property longitude The longitude in degrees.
 * @property altitude The altitude in meters above the WGS84 reference ellipsoid, if available.
 * @property accuracy The estimated radial accuracy in meters.
 * @property bearing The bearing in degrees measured clockwise from true north, if available.
 * @property speed The speed in meters/second over ground, if available.
 */
@Serializable
data class DeviceLocation(
    val uuid: Uuid,
    val userUuid: Uuid,
    val provider: String,
    val fixTime: Instant,
    val deviceTime: Instant,
    val latitude: Double,
    val longitude: Double,
    val altitude: Double?,
    val accuracy: Double,
    val bearing: Double?,
    val speed: Double?,
)
