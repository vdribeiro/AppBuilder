package com.app.builder.domain.permission

import kotlin.uuid.Uuid
import com.app.builder.domain.EntityType
import com.app.builder.domain.Permission

/** Permissions service that manages user permissions. */
interface PermissionService {

    /** Stops listening the instance signal. */
    suspend fun stop()

    /** Starts listening the instance signal. */
    fun start()

    /**
     * Sets the permissions of the user.
     *
     * @param userUuid The user to update.
     * @param permissions The new permissions.
     */
    suspend fun set(userUuid: Uuid, permissions: Map<EntityType, Permission>?)

    /**
     * Returns the user permissions for the given [userUuid].
     *
     * @param userUuid The user to look up.
     * @return The permissions of the user, or null if the user is not found.
     */
    fun get(userUuid: Uuid): Map<EntityType, Permission>?
}
