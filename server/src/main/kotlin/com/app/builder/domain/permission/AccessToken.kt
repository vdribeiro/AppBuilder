package com.app.builder.domain.permission

import kotlin.uuid.Uuid
import com.app.builder.core.security.createAccessToken
import com.app.builder.core.security.getClaimValue
import com.app.builder.data.serializer.decode
import com.app.builder.data.serializer.encode
import com.app.builder.domain.EntityType
import com.app.builder.domain.Permission
import com.auth0.jwt.interfaces.Payload

/** The JWT claim name carrying the user permissions. */
private const val CLAIM_PERMISSIONS = "permissions"

/**
 * Generates a new JWT access token for the given subject, embedding the user's permissions as a claim.
 *
 * @param userUuid The principal identifier to be embedded in the token.
 * @param permissions The user permissions.
 * @return A signed JWT string if successful, or `null` if the token creation fails.
 */
fun createAccessToken(userUuid: Uuid, permissions: Map<EntityType, Permission>): String? =
    createAccessToken(userUuid = userUuid, claimName = CLAIM_PERMISSIONS, claimValue = encode(value = permissions).orEmpty())

/**
 * Decodes the permissions from the JWT claim.
 *
 * @receiver The JWT payload.
 * @return A map of entity types to permissions if successful, or `null` if the decoding fails.
 */
fun Payload.getPermissions(): Map<EntityType, Permission>? =
    decode<Map<EntityType, Permission>>(value = getClaimValue(claimName = CLAIM_PERMISSIONS).orEmpty())
