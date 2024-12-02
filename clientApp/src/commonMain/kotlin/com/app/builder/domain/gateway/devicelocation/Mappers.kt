package com.app.builder.domain.gateway.devicelocation

import com.app.builder.data.database.DeviceLocationSchema
import com.app.builder.domain.DeviceLocation

/**
 * Maps a [DeviceLocationSchema] entity to a [DeviceLocation] domain model.
 *
 * @return The corresponding [DeviceLocation] instance.
 */
fun DeviceLocationSchema.toDeviceLocation(): DeviceLocation = DeviceLocation(
    uuid = uuid,
    userUuid = userUuid,
    provider = provider,
    fixTime = fixTime,
    deviceTime = deviceTime,
    latitude = latitude,
    longitude = longitude,
    altitude = altitude,
    accuracy = accuracy,
    bearing = bearing,
    speed = speed,
)

/**
 * Maps a [DeviceLocation] domain model to a [DeviceLocationSchema] entity.
 *
 * @return The corresponding [DeviceLocationSchema] instance.
 */
fun DeviceLocation.toDeviceLocationSchema(): DeviceLocationSchema = DeviceLocationSchema(
    uuid = uuid,
    userUuid = userUuid,
    provider = provider,
    fixTime = fixTime,
    deviceTime = deviceTime,
    latitude = latitude,
    longitude = longitude,
    altitude = altitude,
    accuracy = accuracy,
    bearing = bearing,
    speed = speed,
)
