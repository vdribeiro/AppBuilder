package com.app.builder.data.database.table

import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.datetime.timestamp

/** Table for storing equipment users. */
object EquipmentUserTable: Table(name = "equipment_user") {
    /** The unique, immutable identifier for the equipment user, derived from its equipment and user. */
    val uuid = uuid(name = "uuid")
    /** The timestamp indicating when this equipment user was last updated. */
    val modifiedAt = timestamp(name = "modified_at")
    /** The timestamp indicating when this equipment user was last deleted. */
    val deletedAt = timestamp(name = "deleted_at").nullable()
    /** The unique identifier of the linked equipment. */
    val equipmentUuid = uuid(name = "equipment_uuid")
    /** The unique identifier of the linked user. */
    val userUuid = uuid(name = "user_uuid")

    override val primaryKey = PrimaryKey(firstColumn = uuid)

    init {
        index(isUnique = false, modifiedAt, uuid)
    }
}
