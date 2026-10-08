package com.app.builder.domain.permission

import kotlin.uuid.Uuid

/** Permissions service that manages user authorization state. */
interface PermissionService {

    /** Stops listening the instance signal. */
    suspend fun stop()

    /** Starts listening the instance signal. */
    fun start()

    /**
     * Sets the authorization state of the user.
     *
     * @param userUuid The user to update.
     * @param access The new authorization state, or null to drop the user from the cache.
     */
    suspend fun set(userUuid: Uuid, access: UserAccess?)

    /**
     * Returns the authorization state for the given [userUuid].
     *
     * @param userUuid The user to look up.
     * @return The cached [UserAccess], or null if the user is not currently cached.
     */
    fun get(userUuid: Uuid): UserAccess?
}
