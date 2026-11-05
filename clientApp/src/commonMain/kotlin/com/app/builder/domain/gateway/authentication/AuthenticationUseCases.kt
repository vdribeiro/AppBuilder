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
     * Authenticates a user using their unique [credentials].
     *
     * @param credentials The user's login credentials.
     * @return The authenticated [User], or null if authentication fails.
     */
    suspend fun login(credentials: UserCredentials): User?

    /**
     * Authenticates a user using their unique [userUuid].
     * The user must have previously authenticated with [UserCredentials].
     *
     * @param userUuid The user unique identifier.
     * @return The authenticated [User], or null if authentication fails.
     */
    suspend fun login(userUuid: Uuid): User?

    /**
     * Executes the session termination workflow.
     *
     * @param userUuid The unique identifier of the user to log out.
     */
    suspend fun logout(userUuid: Uuid)
}