package com.app.builder.data.database.table

import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.datetime.timestamp

/** Table for storing building equipments. */
object BuildingEquipmentTable: Table(name = "building_equipment") {
    /** The unique, immutable identifier for the building equipment, derived from its building and equipment. */
    val uuid = uuid(name = "uuid")
    /** The timestamp indicating when this building equipment was last updated. */
    val modifiedAt = timestamp(name = "modified_at")
    /** The timestamp indicating when this building equipment was last deleted. */
    val deletedAt = timestamp(name = "deleted_at").nullable()
    /** The unique identifier of the linked building. */
    val buildingUuid = uuid(name = "building_uuid")
    /** The unique identifier of the linked equipment. */
    val equipmentUuid = uuid(name = "equipment_uuid")

    override val primaryKey = PrimaryKey(firstColumn = uuid)
}
