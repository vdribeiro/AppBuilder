package com.app.builder.domain.permission

import kotlin.time.Instant
import kotlinx.serialization.Serializable
import com.app.builder.domain.EntityType
import com.app.builder.domain.Permission

/**
 * The authorization state of a user.
 *
 * @property permissions The permissions granted to the user by entity type.
 * @property deletedAt When the account was deleted, or null while it is active.
 */
@Serializable
data class UserAccess(
    val permissions: Map<EntityType, Permission>,
    val deletedAt: Instant?
)
