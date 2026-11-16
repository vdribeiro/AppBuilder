package com.app.builder.data.database.table

import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.datetime.timestamp

/** Table for storing materials. */
object MaterialTable: Table(name = "material") {
    /** The unique, immutable identifier for the material. */
    val uuid = uuid(name = "uuid")
    /** The timestamp indicating when this material was last updated. */
    val modifiedAt = timestamp(name = "modified_at")
    /** The timestamp indicating when this material was last deleted. */
    val deletedAt = timestamp(name = "deleted_at").nullable()
    /** The name of the material. */
    val name = text(name = "name")
    /** The descriptive content detailing the material. */
    val description = text(name = "description")

    override val primaryKey = PrimaryKey(firstColumn = uuid)
}