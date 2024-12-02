package com.app.builder.data.database.table

import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.datetime.timestamp

/** Table for storing tasks. */
object TaskTable: Table(name = "task") {
    /** The unique, immutable identifier for the task. */
    val uuid = uuid(name = "uuid")
    /** The timestamp indicating when this task was last updated. */
    val modifiedAt = timestamp(name = "modified_at")
    /** The timestamp indicating when this task was last deleted. */
    val deletedAt = timestamp(name = "deleted_at").nullable()
    /** The title of the task. */
    val title = text(name = "title")
    /** The descriptive content detailing the task. */
    val description = text(name = "description")
    /** The state of the task. */
    val state = text(name = "state")

    override val primaryKey = PrimaryKey(firstColumn = uuid)
}