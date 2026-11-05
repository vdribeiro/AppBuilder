package com.app.builder.data.database.table

import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.datetime.timestamp

/** Table for storing user accounts, holding the login credentials and permissions of a user. */
object UserAccountTable: Table(name = "user_account") {
    /** The unique identifier of the user the account belongs to, which also identifies the account. */
    val userUuid = reference(name = "user_uuid", refColumn = UserTable.uuid)
    /** The timestamp indicating when this account was last updated. */
    val modifiedAt = timestamp(name = "modified_at")
    /** The timestamp indicating when this account was disabled, or null while it is active. */
    val deletedAt = timestamp(name = "deleted_at").nullable()
    /** The account's unique login username. */
    val username = text(name = "username").uniqueIndex()
    /** The hashed password used to authenticate the account. */
    val passwordHash = text(name = "password_hash")
    /** The permissions granted to this account, stored as JSON-encoded text and keyed by entity type. */
    val permissions = text(name = "permissions")

    override val primaryKey = PrimaryKey(firstColumn = userUuid)
}
