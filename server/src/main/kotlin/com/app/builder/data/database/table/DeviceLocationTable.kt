package com.app.builder.data.database.table

import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.datetime.timestamp

/** Table for storing device location fixes. */
object DeviceLocationTable: Table(name = "device_location") {
    /** The unique identifier of this fix, generated on-device so retried deliveries stay idempotent. */
    val uuid = uuid(name = "uuid")
    /** The unique identifier of the user who owns the device that reported this fix. */
    val userUuid = reference(name = "user_uuid", refColumn = UserTable.uuid)
    /** The source of the fix, e.g. "gps", "network", "fused". */
    val provider = text(name = "provider")
    /** The Unix epoch time of this location fix recorded by the location provider in milliseconds since the start of the Unix epoch. */
    val fixTime = timestamp(name = "fix_time")
    /** The Unix epoch time of this location fix recorded by the device clock in milliseconds since the start of the Unix epoch. */
    val deviceTime = timestamp(name = "device_time")
    /** The latitude in degrees. */
    val latitude = double(name = "latitude")
    /** The longitude in degrees. */
    val longitude = double(name = "longitude")
    /** The altitude in meters above the WGS84 reference ellipsoid, if available. */
    val altitude = double(name = "altitude").nullable()
    /** The estimated radial accuracy in meters. */
    val accuracy = double(name = "accuracy")
    /** The bearing in degrees measured clockwise from true north, if available. */
    val bearing = double(name = "bearing").nullable()
    /** The speed in meters/second over ground, if available. */
    val speed = double(name = "speed").nullable()

    override val primaryKey = PrimaryKey(firstColumn = uuid)
}
