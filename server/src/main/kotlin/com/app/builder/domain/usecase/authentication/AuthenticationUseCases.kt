package com.app.builder.domain.usecase.authentication

import kotlin.uuid.Uuid
import com.app.builder.core.config.ServerConfigs
import com.app.builder.domain.Authentication
import com.app.builder.domain.BearerToken
import com.app.builder.domain.PasswordChange
import com.app.builder.domain.RegistrationForm
import com.app.builder.domain.UserCredentials
import com.app.builder.domain.usecase.Outcome

/** Defines the business workflows for user registration, login, and session management. */
interface AuthenticationUseCases {

    /**
     * Creates the account of a submitted [registrationForm], storing its user too when it doesn't exist yet. An existing user is left unchanged. No session is created for the new account.
     *
     * @param registrationForm The registration details.
     * @return [RegisterOutcome.Success] when created, [RegisterOutcome.InvalidPassword] when the password is blank, shorter than [ServerConfigs.minPasswordLength] or longer than 72 bytes, or the other [RegisterOutcome] telling why it wasn't.
     */
    suspend fun register(registrationForm: RegistrationForm): RegisterOutcome

    /**
     * Authenticates a user using their unique [credentials], opening a new session.
     * Failed logins are counted per username, and a username that reaches [ServerConfigs.maxLoginAttempts] is refused for [ServerConfigs.loginLockDuration], even with the right password. A successful login clears the count.
     *
     * @param credentials The user's login credentials.
     * @return [LoginOutcome.Success] with the [Authentication], or the [LoginOutcome] telling why the login failed.
     */
    suspend fun login(credentials: UserCredentials): LoginOutcome

    /**
     * Invalidates the session associated with [refreshToken].
     *
     * @param refreshToken The refresh token identifying the session to terminate.
     * @return [Outcome.Success] with the uuid of the user the session belonged to, [Outcome.Rejected] when no session matches, or [Outcome.Error] when the operation failed.
     */
    suspend fun logout(refreshToken: String): Outcome<Uuid>

    /**
     * Issues a new access token using a valid [refreshToken], rotating it in the process.
     * The session of a disabled account never expires later than [ServerConfigs.deletedAccountSessionValidity] after the account was disabled.
     *
     * @param refreshToken The long-lived token previously issued during login.
     * @return [Outcome.Success] with a fresh [BearerToken] pair, [Outcome.Rejected] when the refresh token is unknown or expired, or the account was disabled longer ago than [ServerConfigs.deletedAccountSessionValidity], or [Outcome.Error] when the operation failed.
     */
    suspend fun refreshTokens(refreshToken: String): Outcome<BearerToken>

    /**
     * Changes the password of the account the session of a submitted [passwordChange] belongs to, and invalidates every other session of that account.
     * A session that gives [ServerConfigs.maxPasswordAttempts] wrong current passwords without a successful change in between is ended.
     *
     * @param passwordChange The refresh token of the session, the current password and the new one.
     * @return [PasswordChangeOutcome.Success] with the uuid of the user the account belongs to, [PasswordChangeOutcome.InvalidPassword] when the current password is wrong, with the attempts left, or the new one is blank, shorter than [ServerConfigs.minPasswordLength] or longer than 72 bytes, or the other [PasswordChangeOutcome] telling why it wasn't changed.
     */
    suspend fun changePassword(passwordChange: PasswordChange): PasswordChangeOutcome
}
