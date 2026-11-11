package com.app.builder.data.database.table

import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.datetime.timestamp

/** Table for storing buildings. */
object BuildingTable: Table(name = "building") {
    /** The unique, immutable identifier for the building. */
    val uuid = uuid(name = "uuid")
    /** The timestamp indicating when this building was last updated. */
    val modifiedAt = timestamp(name = "modified_at")
    /** The timestamp indicating when this building was last deleted. */
    val deletedAt = timestamp(name = "deleted_at").nullable()
    /** The name of the building. */
    val name = text(name = "name")
    /** The descriptive content detailing the building. */
    val description = text(name = "description")
    /** The postal address of the building. */
    val address = text(name = "address")
    /** The latitude of the building in degrees, or `null` when unknown. */
    val latitude = double(name = "latitude").nullable()
    /** The longitude of the building in degrees, or `null` when unknown. */
    val longitude = double(name = "longitude").nullable()

    override val primaryKey = PrimaryKey(firstColumn = uuid)
}