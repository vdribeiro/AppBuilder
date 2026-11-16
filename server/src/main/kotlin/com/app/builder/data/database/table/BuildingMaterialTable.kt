package com.app.builder.data.database.table

import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.datetime.timestamp

/** Table for storing building materials. */
object BuildingMaterialTable: Table(name = "building_material") {
    /** The unique, immutable identifier for the building material, derived from its building and material. */
    val uuid = uuid(name = "uuid")
    /** The timestamp indicating when this building material was last updated. */
    val modifiedAt = timestamp(name = "modified_at")
    /** The timestamp indicating when this building material was last deleted. */
    val deletedAt = timestamp(name = "deleted_at").nullable()
    /** The unique identifier of the linked building. */
    val buildingUuid = uuid(name = "building_uuid")
    /** The unique identifier of the linked material. */
    val materialUuid = uuid(name = "material_uuid")
    /** The quantity of the material stored in the building. */
    val quantity = integer(name = "quantity")

    override val primaryKey = PrimaryKey(firstColumn = uuid)
}
