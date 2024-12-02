package com.app.builder.domain.usecase.authentication

import kotlin.uuid.Uuid
import com.app.builder.domain.RegistrationForm
import com.app.builder.domain.UserCredentials
import com.app.builder.domain.Authentication
import com.app.builder.domain.BearerToken

/** Defines the business workflows for user registration, login, and session management. */
interface AuthenticationUseCases {

    /**
     * Creates a new user account using a submitted [registrationForm].
     *
     * @param registrationForm The user's registration details.
     * @return The [Authentication], or null if registration fails.
     */
    suspend fun register(registrationForm: RegistrationForm): Authentication?

    /**
     * Authenticates a user using their unique [credentials].
     *
     * @param credentials The user's login credentials.
     * @return The [Authentication], or null if authentication fails.
     */
    suspend fun login(credentials: UserCredentials): Authentication?

    /**
     * Invalidates the session associated with [refreshToken].
     *
     * @param refreshToken The refresh token identifying the session to terminate.
     * @return The uuid of the user the session belonged to, or null if the session was not found or the operation failed.
     */
    suspend fun logout(refreshToken: String): Uuid?

    /**
     * Issues a new access token using a valid [refreshToken], rotating it in the process.
     *
     * @param refreshToken The long-lived token previously issued during login or registration.
     * @return A fresh [BearerToken] pair, or null if the refresh token is invalid or expired.
     */
    suspend fun refreshTokens(refreshToken: String): BearerToken?
}
