package com.app.builder.data.database.table

import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.datetime.timestamp
import com.app.builder.core.config.ServerConfigs

/** Table for storing user sessions. */
object UserSessionTable: Table(name = "user_session") {
    /** The unique identifier of the session. */
    val uuid = uuid(name = "uuid")
    /** The unique identifier of the user the session belongs to. */
    val userUuid = reference(name = "user_uuid", refColumn = UserTable.uuid)
    /** The SHA-256 hash of the refresh token used to obtain new access tokens for this session. */
    val refreshToken = text(name = "refresh_token").uniqueIndex()
    /** The SHA-256 hash of the refresh token [refreshToken] replaced at its last rotation, still accepted for [ServerConfigs.refreshTokenGracePeriod] after [rotatedAt]. */
    val previousRefreshToken = text(name = "previous_refresh_token").nullable().index()
    /** The timestamp of the last rotation of [refreshToken], or `null` if it was never rotated. */
    val rotatedAt = timestamp(name = "rotated_at").nullable()
    /** The timestamp indicating when the session was created. */
    val createdAt = timestamp(name = "created_at")
    /** The timestamp after which the session is no longer valid. */
    val expiresAt = timestamp(name = "expires_at")

    override val primaryKey = PrimaryKey(firstColumn = uuid)
}
