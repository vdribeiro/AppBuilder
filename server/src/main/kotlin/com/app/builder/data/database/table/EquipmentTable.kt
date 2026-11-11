package com.app.builder.data.database.table

import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.datetime.timestamp

/** Table for storing equipments. */
object EquipmentTable: Table(name = "equipment") {
    /** The unique, immutable identifier for the equipment. */
    val uuid = uuid(name = "uuid")
    /** The timestamp indicating when this equipment was last updated. */
    val modifiedAt = timestamp(name = "modified_at")
    /** The timestamp indicating when this equipment was last deleted. */
    val deletedAt = timestamp(name = "deleted_at").nullable()
    /** The name of the equipment. */
    val name = text(name = "name")
    /** The descriptive content detailing the equipment. */
    val description = text(name = "description")
    /** The operating status of the equipment. */
    val status = text(name = "status")

    override val primaryKey = PrimaryKey(firstColumn = uuid)
}