package com.app.builder.domain.permission

import kotlin.uuid.Uuid
import com.app.builder.core.security.createAccessToken
import com.app.builder.core.security.getClaimValue
import com.app.builder.data.serializer.decode
import com.app.builder.data.serializer.encode
import com.auth0.jwt.interfaces.Payload

/** The JWT claim name carrying the user's authorization state. */
private const val CLAIM_ACCESS = "access"

/**
 * Generates a new JWT access token for the given subject, embedding the user's authorization state as a claim.
 *
 * @param userUuid The principal identifier to be embedded in the token.
 * @param access The user's permissions and deleted state.
 * @return A signed JWT string if successful, or `null` if the token creation fails.
 */
fun createAccessToken(userUuid: Uuid, access: UserAccess): String? =
    createAccessToken(userUuid = userUuid, claimName = CLAIM_ACCESS, claimValue = encode(value = access).orEmpty())

/**
 * Decodes the authorization state from the JWT claim.
 *
 * @receiver The JWT payload.
 * @return The [UserAccess] if successful, or `null` if the decoding fails.
 */
fun Payload.getAccess(): UserAccess? =
    decode<UserAccess>(value = getClaimValue(claimName = CLAIM_ACCESS).orEmpty())
