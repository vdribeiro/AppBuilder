package com.app.builder.data.database.table

import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.datetime.timestamp

/** Table for storing time punches. */
object TimePunchTable: Table(name = "time_punch") {
    /** The unique, immutable identifier for the time punch. */
    val uuid = uuid(name = "uuid")
    /** The timestamp indicating when this time punch was last updated. */
    val modifiedAt = timestamp(name = "modified_at")
    /** The timestamp indicating when this time punch was last deleted. */
    val deletedAt = timestamp(name = "deleted_at").nullable()
    /** The unique identifier of the user that punched the clock. */
    val userUuid = uuid(name = "user_uuid")
    /** The timestamp at which the user punched the clock. */
    val time = timestamp(name = "time")
    /** The source of the time punch. */
    val punchSource = text(name = "source")

    override val primaryKey = PrimaryKey(firstColumn = uuid)

    init {
        index(isUnique = false, modifiedAt, uuid)
        index(isUnique = false, userUuid, modifiedAt, uuid)
    }
}
