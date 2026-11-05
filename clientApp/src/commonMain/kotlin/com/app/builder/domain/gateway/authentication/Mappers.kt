package com.app.builder.domain.gateway.authentication

import io.ktor.client.plugins.auth.providers.BearerTokens
import com.app.builder.core.security.decrypt
import com.app.builder.core.security.deriveKey
import com.app.builder.core.security.encrypt
import com.app.builder.data.database.AccountSchema
import com.app.builder.data.database.SessionSchema
import com.app.builder.data.database.UserSchema
import com.app.builder.data.serializer.decode
import com.app.builder.data.serializer.encode
import com.app.builder.domain.Account
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
    name = name,
    avatar = avatar,
)

/**
 * Maps an [AccountSchema] entity to an [Account] domain model.
 *
 * @return The corresponding [Account] instance.
 */
fun AccountSchema.toAccount(): Account = Account(
    userUuid = userUuid,
    modifiedAt = modifiedAt,
    deletedAt = deletedAt,
    username = username,
    permissions = decode<Map<EntityType, Permission>>(value = permissions).orEmpty(),
)

/**
 * Maps an [Account] domain model to an [AccountSchema] entity.
 *
 * @return The corresponding [AccountSchema] instance.
 */
fun Account.toAccountSchema(): AccountSchema = AccountSchema(
    userUuid = userUuid,
    modifiedAt = modifiedAt,
    deletedAt = deletedAt,
    username = username,
    permissions = encode(value = permissions).orEmpty(),
)

/**
 * Maps a [UserSession] to a [SessionSchema]
 *
 * @return The corresponding [SessionSchema] instance.
 */
fun UserSession.toSessionSchema(): SessionSchema = SessionSchema(
    userUuid = user.uuid,
    username = account.username,
    salt = salt,
    verifier = verifier,
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
 * Derives the verifier for these [UserCredentials].
 *
 * @param salt The random salt to derive with.
 * @return The derived verifier, or `null` if derivation failed.
 */
suspend fun UserCredentials.deriveVerifier(salt: String): String? =
    deriveKey(password = password, salt = salt)?.takeIf { it.isNotBlank() }
