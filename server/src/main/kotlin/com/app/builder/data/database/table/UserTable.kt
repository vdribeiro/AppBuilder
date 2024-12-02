package com.app.builder.data.database.table

import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.datetime.timestamp

/** Table for storing users. */
object UserTable: Table(name = "user") {
    /** The unique identifier for the user. */
    val uuid = uuid(name = "uuid")
    /** The timestamp indicating when this user was last updated. */
    val modifiedAt = timestamp(name = "modified_at")
    /** The timestamp indicating when this user was last deleted. */
    val deletedAt = timestamp(name = "deleted_at").nullable()
    /** The permissions granted to this user, stored as JSON-encoded text and keyed by entity type. */
    val permissions = text(name = "permissions")
    /** The display name of the user. */
    val name = text(name = "name")
    /** The URL to the user's profile image, or null if not set. */
    val avatar = text(name = "avatar").nullable()

    override val primaryKey = PrimaryKey(firstColumn = uuid)
}
