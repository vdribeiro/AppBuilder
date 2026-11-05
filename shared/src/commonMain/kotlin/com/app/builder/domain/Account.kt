package com.app.builder.domain

import kotlin.time.Instant
import kotlin.uuid.Uuid
import kotlinx.serialization.Serializable

/**
 * The login account of a [User], holding what the user is allowed to do.
 *
 * @property userUuid The unique identifier of the user the account belongs to, which also identifies the account.
 * @property modifiedAt The timestamp indicating when this account was last updated.
 * @property deletedAt The timestamp indicating when this account was disabled, or null while it is active.
 * @property username The unique login username.
 * @property permissions The permissions granted to this account by entity type. If the entity is not listed, then the account has no access to it.
 */
@Serializable
data class Account(
    val userUuid: Uuid,
    val modifiedAt: Instant,
    val deletedAt: Instant?,
    val username: String,
    val permissions: Map<EntityType, Permission>,
) {
    /**
     * Checks if the account has a given [permission] for a given [entityType].
     * A [Permission.WRITE] permission also means that an account can [Permission.READ].
     *
     * @param entityType to check.
     * @param permission to check.
     * @return true if the account has permission, false otherwise.
     */
    fun hasPermission(entityType: EntityType, permission: Permission): Boolean =
        when (permissions[entityType]) {
            Permission.WRITE -> true
            Permission.READ -> permission == Permission.READ
            else -> false
        }
}
