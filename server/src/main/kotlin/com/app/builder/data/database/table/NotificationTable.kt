package com.app.builder.data.database.table

import org.jetbrains.exposed.v1.core.Table

/** Table for storing notifications. */
object NotificationTable: Table(name = "notification") {
    /** The unique identifier of the notification. */
    val uuid = uuid(name = "uuid")
    /** The unique identifier of the user the notification is addressed to. */
    val userUuid = reference(name = "user_uuid", refColumn = UserTable.uuid)
    /** The title of the notification. */
    val title = text(name = "title")
    /** The descriptive content of the notification. */
    val description = text(name = "description")
    /** The type of the entity the notification is about. */
    val entityType = text(name = "entity_type")
    /** The unique identifier of the entity the notification is about, if any. */
    val entityUuid = uuid(name = "entity_uuid").nullable()

    override val primaryKey = PrimaryKey(firstColumn = uuid)
}