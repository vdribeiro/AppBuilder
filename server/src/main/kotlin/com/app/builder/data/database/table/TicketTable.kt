package com.app.builder.data.database.table

import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.datetime.timestamp

/** Table for storing single-use, short-lived push connection tickets. */
object TicketTable: Table(name = "ticket") {
    /** The unique identifier of the ticket. */
    val uuid = uuid(name = "uuid")
    /** The unique identifier of the user the ticket was issued to. */
    val userUuid = reference(name = "user_uuid", refColumn = UserTable.uuid)
    /** The timestamp after which the ticket is no longer valid. */
    val expiresAt = timestamp(name = "expires_at")

    override val primaryKey = PrimaryKey(firstColumn = uuid)
}
