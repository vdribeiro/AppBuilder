package com.app.builder.data.database.table

import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.datetime.timestamp

/** Table for storing user sessions. */
object UserSessionTable: Table(name = "user_session") {
    /** The unique identifier of the session. */
    val uuid = uuid(name = "uuid")
    /** The unique identifier of the user the session belongs to. */
    val userUuid = reference(name = "user_uuid", refColumn = UserTable.uuid)
    /** The refresh token used to obtain new access tokens for this session. */
    val refreshToken = text(name = "refresh_token").uniqueIndex()
    /** The timestamp indicating when the session was created. */
    val createdAt = timestamp(name = "created_at")
    /** The timestamp after which the session is no longer valid. */
    val expiresAt = timestamp(name = "expires_at")

    override val primaryKey = PrimaryKey(firstColumn = uuid)
}
