package com.app.builder.domain.gateway.authentication

import io.ktor.client.plugins.auth.providers.BearerTokens
import com.app.builder.core.security.decrypt
import com.app.builder.core.security.encrypt
import com.app.builder.core.security.hash
import com.app.builder.data.database.SessionSchema
import com.app.builder.data.database.UserSchema
import com.app.builder.data.serializer.decode
import com.app.builder.data.serializer.encode
import com.app.builder.domain.BearerToken
import com.app.builder.domain.EntityType
import com.app.builder.domain.Permission
import com.app.builder.domain.User
import com.app.builder.domain.UserCredentials
import com.app.builder.domain.UserSession

/**
 * Maps a [UserSchema] entity to a [User] domain model.
 *
 * @return The corresponding [User] instance.
 */
fun UserSchema.toUser(): User = User(
    uuid = uuid,
    modifiedAt = modifiedAt,
    deletedAt = deletedAt,
    permissions = decode<Map<EntityType, Permission>>(value = permissions).orEmpty(),
    name = name,
    avatar = avatar,
)

/**
 * Maps a [User] domain model to a [UserSchema] entity.
 *
 * @return The corresponding [UserSchema] instance.
 */
fun User.toUserSchema(): UserSchema = UserSchema(
    uuid = uuid,
    modifiedAt = modifiedAt,
    deletedAt = deletedAt,
    permissions = encode(value = permissions).orEmpty(),
    name = name,
    avatar = avatar,
)

/**
 * Maps a [UserSession] to a [SessionSchema]
 *
 * @return The corresponding [SessionSchema] instance.
 */
fun UserSession.toSessionSchema(): SessionSchema = SessionSchema(
    userUuid = user.uuid,
    hash = hash,
    accessToken = accessToken,
    refreshToken = refreshToken
)

/**
 * Maps a [SessionSchema] entity to a [BearerToken] domain model.
 *
 * @return The corresponding [BearerToken] instance.
 */
fun SessionSchema.toBearerToken(): BearerToken = BearerToken(
    accessToken = accessToken,
    refreshToken = refreshToken
)

/**
 * Encrypts a [BearerToken].
 *
 * @return The encrypted [BearerToken], or `null` if encryption failed.
 */
suspend fun BearerToken.encrypt(): BearerToken? {
    val accessToken = encrypt(content = accessToken)
    if (accessToken.isNullOrBlank()) return null
    val refreshToken = encrypt(content = refreshToken)
    if (refreshToken.isNullOrBlank()) return null
    return BearerToken(
        accessToken = accessToken,
        refreshToken = refreshToken
    )
}

/**
 * Decrypts a [BearerToken].
 *
 * @return The decrypted [BearerToken], or `null` if decryption failed.
 */
suspend fun BearerToken.decrypt(): BearerToken? {
    val accessToken = decrypt(content = accessToken)
    if (accessToken.isNullOrBlank()) return null
    val refreshToken = decrypt(content = refreshToken)
    if (refreshToken.isNullOrBlank()) return null
    return BearerToken(
        accessToken = accessToken,
        refreshToken = refreshToken
    )
}

/**
 * Maps a [BearerToken] domain model to [BearerTokens].
 *
 * @return The corresponding [BearerTokens] instance.
 */
fun BearerToken.toBearerTokens(): BearerTokens = BearerTokens(
    accessToken = accessToken,
    refreshToken = refreshToken
)

/**
 * Hash [UserCredentials].
 *
 * @return The resulting hash, or `null` if hashing failed.
 */
suspend fun UserCredentials.hash(): String? {
    val hash = hash(content = "$username$password")
    if (hash.isNullOrBlank()) return null
    return hash
}
