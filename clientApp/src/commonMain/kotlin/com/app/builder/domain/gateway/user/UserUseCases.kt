package com.app.builder.domain.gateway.user

import kotlin.uuid.Uuid
import kotlinx.coroutines.flow.Flow
import com.app.builder.domain.User

/** Defines the business workflows for users. */
interface UserUseCases {

    /**
     * Observes the complete list of users.
     *
     * @return [Flow] emitting the current list of [User] objects.
     */
    fun observeUsers(): Flow<List<User>>

    /**
     * Observes a specific application user by its unique identifier.
     *
     * @param uuid The unique identifier of the user to observe.
     * @return [Flow] emitting the [User] if found, or null if it does not exist.
     */
    fun observeUser(uuid: Uuid): Flow<User?>

    /**
     * Inserts a new user or updates an existing one.
     *
     * @param user The [User] to be inserted or updated.
     */
    suspend fun upsertUser(user: User)
}
