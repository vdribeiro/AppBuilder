package com.app.builder.domain

import kotlin.time.Instant
import kotlin.uuid.Uuid
import kotlinx.serialization.Serializable

/**
 * User profile representation.
 *
 * @property uuid The unique identifier for the user.
 * @property modifiedAt The timestamp indicating when this user was last updated.
 * @property deletedAt The timestamp indicating when this user was last deleted.
 * @property permissions The permissions granted to this user by entity type. If the entity is not listed, then the user has no access to it.
 * @property name The display name of the user.
 * @property avatar The URL to the user's profile image, or null if not set.
 */
@Serializable
data class User(
    val uuid: Uuid,
    val modifiedAt: Instant,
    val deletedAt: Instant?,
    val permissions: Map<EntityType, Permission>,
    val name: String,
    val avatar: String?,
) {
    /**
     * Checks if the user has a given [permission] for a given [entityType].
     * A [Permission.WRITE] permission also means that a user can [Permission.READ].
     *
     * @param entityType to check.
     * @param permission to check.
     * @return true if the user has permission, false otherwise.
     */
    fun hasPermission(entityType: EntityType, permission: Permission): Boolean =
        when (permissions[entityType]) {
            Permission.WRITE -> true
            Permission.READ -> permission == Permission.READ
            else -> false
        }
}