package com.app.builder.domain.gateway.authentication

import kotlin.uuid.Uuid
import kotlinx.coroutines.flow.Flow
import com.app.builder.domain.Account
import com.app.builder.domain.User
import com.app.builder.domain.UserCredentials

/** Defines the core contract for managing user authentication. */
interface AuthenticationUseCases {

    /**
     * Observes the active user session.
     * Emits the authenticated user, or null if no user is authenticated or the session expires.
     *
     * @return A [Flow] emitting the user or null.
     */
    fun observeCurrentUser(): Flow<User?>

    /**
     * Observes the account of the active user session.
     * Emits the authenticated user's account, or null if no user is authenticated or the session expires.
     *
     * @return A [Flow] emitting the account or null.
     */
    fun observeCurrentAccount(): Flow<Account?>

    /**
     * Observes whether the server refused the session of the active user.
     *
     * @return A [Flow] emitting `true` while the session is expired, and `false` otherwise or when no user is authenticated.
     */
    fun observeSessionExpired(): Flow<Boolean>

    /**
     * Authenticates a user using their unique [credentials].
     *
     * @param credentials The user's login credentials.
     * @return The authenticated [User], or null if authentication fails.
     */
    suspend fun login(credentials: UserCredentials): User?

    /**
     * Executes the session termination workflow.
     *
     * @param userUuid The unique identifier of the user to log out.
     */
    suspend fun logout(userUuid: Uuid)
}