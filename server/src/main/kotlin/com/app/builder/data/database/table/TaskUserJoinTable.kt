package com.app.builder.data.database.table

import org.jetbrains.exposed.v1.core.Table

/** Table for storing task and user relationship. */
object TaskUserJoinTable: Table(name = "task_user_join") {
    /** The unique identifier of the linked task. */
    val taskUuid = uuid(name = "task_uuid")
    /** The unique identifier of the linked user. */
    val userUuid = uuid(name = "user_uuid")

    override val primaryKey = PrimaryKey(taskUuid, userUuid)
}