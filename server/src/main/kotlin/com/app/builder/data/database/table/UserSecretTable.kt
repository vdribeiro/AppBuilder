package com.app.builder.data.database.table

import org.jetbrains.exposed.v1.core.Table

/** Table for storing user secrets. */
object UserSecretTable: Table(name = "user_secret") {
    /** The unique identifier of the user the credentials belong to. */
    val userUuid = reference(name = "user_uuid", refColumn = UserTable.uuid)
    /** The user's unique login username. */
    val username = text(name = "username").uniqueIndex()
    /** The hashed password used to authenticate the user. */
    val passwordHash = text(name = "password_hash")

    override val primaryKey = PrimaryKey(firstColumn = userUuid)
}
