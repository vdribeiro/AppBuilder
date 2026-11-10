package com.app.builder.domain.usecase.authentication

import kotlin.uuid.Uuid
import com.app.builder.core.config.ServerConfigs
import com.app.builder.domain.Authentication
import com.app.builder.domain.BearerToken
import com.app.builder.domain.RegistrationForm
import com.app.builder.domain.UserCredentials
import com.app.builder.domain.usecase.Outcome

/** Defines the business workflows for user registration, login, and session management. */
interface AuthenticationUseCases {

    /**
     * Creates the account of a submitted [registrationForm], storing its user too when it doesn't exist yet. An existing user is left unchanged. No session is created for the new account.
     *
     * @param registrationForm The registration details.
     * @return [Outcome.Success] when created, [Outcome.Invalid] when the account belongs to another user, [Outcome.Conflict] when the user already has an account or the username is taken, or [Outcome.Error] when the operation failed.
     */
    suspend fun register(registrationForm: RegistrationForm): Outcome<Unit>

    /**
     * Authenticates a user using their unique [credentials], opening a new session.
     *
     * @param credentials The user's login credentials.
     * @return [Outcome.Success] with the [Authentication], [Outcome.Rejected] when the user is unknown, the password is wrong or the account is disabled, or [Outcome.Error] when the operation failed.
     */
    suspend fun login(credentials: UserCredentials): Outcome<Authentication>

    /**
     * Invalidates the session associated with [refreshToken].
     *
     * @param refreshToken The refresh token identifying the session to terminate.
     * @return [Outcome.Success] with the uuid of the user the session belonged to, [Outcome.Rejected] when no session matches, or [Outcome.Error] when the operation failed.
     */
    suspend fun logout(refreshToken: String): Outcome<Uuid>

    /**
     * Issues a new access token using a valid [refreshToken], rotating it in the process.
     * The refresh token it replaces stays accepted for [ServerConfigs.refreshTokenGracePeriod], so a client that never received the rotated pair can still recover:
     * presenting it again issues a new access token with the session's current refresh token, without rotating again.
     *
     * @param refreshToken The long-lived token previously issued during login.
     * @return [Outcome.Success] with a fresh [BearerToken] pair, [Outcome.Rejected] when the refresh token is unknown or expired, or [Outcome.Error] when the operation failed.
     */
    suspend fun refreshTokens(refreshToken: String): Outcome<BearerToken>
}
