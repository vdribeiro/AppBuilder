package com.app.builder.data.database.table

import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.datetime.timestamp

/** Table for storing building users. */
object BuildingUserTable: Table(name = "building_user") {
    /** The unique, immutable identifier for the building user, derived from its building and user. */
    val uuid = uuid(name = "uuid")
    /** The timestamp indicating when this building user was last updated. */
    val modifiedAt = timestamp(name = "modified_at")
    /** The timestamp indicating when this building user was last deleted. */
    val deletedAt = timestamp(name = "deleted_at").nullable()
    /** The unique identifier of the linked building. */
    val buildingUuid = uuid(name = "building_uuid")
    /** The unique identifier of the linked user. */
    val userUuid = uuid(name = "user_uuid")

    override val primaryKey = PrimaryKey(firstColumn = uuid)
}
