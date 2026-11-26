package com.app.builder.domain.usecase.authentication

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotEquals
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Instant
import kotlinx.coroutines.flow.firstOrNull
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.r2dbc.selectAll
import com.app.builder.core.config.ServerConfigs
import com.app.builder.core.locale.now
import com.app.builder.core.security.createVerifier
import com.app.builder.core.security.hashRefreshToken
import com.app.builder.core.security.uuid
import com.app.builder.data.database.create
import com.app.builder.data.database.reset
import com.app.builder.data.database.safeTransaction
import com.app.builder.data.database.table.UserSessionTable
import com.app.builder.domain.BearerToken
import com.app.builder.domain.EntityType
import com.app.builder.domain.PasswordChange
import com.app.builder.domain.PasswordFailure
import com.app.builder.domain.Permission
import com.app.builder.domain.UserCredentials
import com.app.builder.domain.permission.UserAccess
import com.app.builder.domain.permission.getAccess
import com.app.builder.domain.usecase.Outcome
import com.app.builder.test.FakeData
import com.app.builder.test.TestCase

class AuthenticationUseCasesTest: TestCase() {

    /** Verifies that a registered account can log in with correct credentials and is rejected with wrong ones, and that registering opens no session. */
    @Test
    fun registerLogin() = runServerTest {
        dependency.get().database.reset()
        dependency.get().database.create()

        val authenticationUseCases = dependency.get()
            .useCases
            .authenticationUseCases

        assertEquals(expected = LoginOutcome.Rejected, actual = authenticationUseCases.login(credentials = FakeData.credentials))

        val registerResult = authenticationUseCases.register(registrationForm = FakeData.registrationForm)
        assertEquals(expected = RegisterOutcome.Success, actual = registerResult)

        val loginResult = assertIs<LoginOutcome.Success>(value = authenticationUseCases.login(credentials = FakeData.credentials)).authentication
        assertEquals(expected = FakeData.user, actual = loginResult.user)
        assertEquals(expected = FakeData.account, actual = loginResult.account)

        assertEquals(expected = LoginOutcome.Rejected, actual = authenticationUseCases.login(credentials = UserCredentials(username = FakeData.credentials.username, password = "wrong_password")))
    }

    /** Verifies that registering is rejected when the user already has an account, the username is taken, or the account belongs to another user. */
    @Test
    fun registerConflicts() = runServerTest {
        val authenticationUseCases = dependency.get()
            .useCases
            .authenticationUseCases

        assertEquals(expected = RegisterOutcome.Conflict, actual = authenticationUseCases.register(registrationForm = FakeData.registrationForm.copy(account = FakeData.account.copy(username = "another_username"))))

        val form = registrationForm(username = FakeData.account.username, permissions = emptyMap())
        assertEquals(expected = RegisterOutcome.Conflict, actual = authenticationUseCases.register(registrationForm = form))
        assertEquals(expected = RegisterOutcome.Invalid, actual = authenticationUseCases.register(registrationForm = form.copy(account = form.account.copy(userUuid = uuid(), username = "new_username"))))
        assertEquals(expected = Outcome.NotFound, actual = dependency.get().useCases.userUseCases.getUser(uuid = form.user.uuid))
    }

    /** Verifies that registering with an empty, blank, too short or too long password is invalid and stores neither the user nor the account, while the shortest and longest allowed passwords are accepted. */
    @Test
    fun registerRejectsInvalidPassword() = runServerTest {
        val useCases = dependency.get().useCases
        val form = registrationForm(username = "invalid_password", permissions = emptyMap())

        val minLength = ServerConfigs.value.minPasswordLength
        listOf("", " ".repeat(n = minLength), "a".repeat(n = minLength - 1)).forEach { password ->
            assertEquals(expected = RegisterOutcome.InvalidPassword(failure = PasswordFailure.TooShort(minLength = minLength)), actual = useCases.authenticationUseCases.register(registrationForm = form.copy(password = password)))
        }
        listOf("a".repeat(n = 73), "é".repeat(n = 37)).forEach { password ->
            assertEquals(expected = RegisterOutcome.InvalidPassword(failure = PasswordFailure.TooLong), actual = useCases.authenticationUseCases.register(registrationForm = form.copy(password = password)))
        }
        assertEquals(expected = Outcome.NotFound, actual = useCases.userUseCases.getUser(uuid = form.user.uuid))
        assertEquals(expected = Outcome.NotFound, actual = useCases.accountUseCases.getAccount(userUuid = form.user.uuid))

        assertEquals(expected = RegisterOutcome.Success, actual = useCases.authenticationUseCases.register(registrationForm = form.copy(password = "a".repeat(n = minLength))))
        val longest = registrationForm(username = "longest_password", permissions = emptyMap()).copy(password = "a".repeat(n = 72))
        assertEquals(expected = RegisterOutcome.Success, actual = useCases.authenticationUseCases.register(registrationForm = longest))
        assertIs<LoginOutcome.Success>(value = useCases.authenticationUseCases.login(credentials = longest.credentials))
    }

    /** Verifies that the minimum password length follows the server configs. */
    @Test
    fun registerFollowsMinPasswordLength() = runServerTest {
        val authenticationUseCases = dependency.get().useCases.authenticationUseCases
        val form = registrationForm(username = "configured_password", permissions = emptyMap()).copy(password = "a".repeat(n = 20))

        ServerConfigs.set { it.copy(minPasswordLength = 21) }
        assertEquals(expected = RegisterOutcome.InvalidPassword(failure = PasswordFailure.TooShort(minLength = 21)), actual = authenticationUseCases.register(registrationForm = form))
        ServerConfigs.set { it.copy(minPasswordLength = 20) }
        assertEquals(expected = RegisterOutcome.Success, actual = authenticationUseCases.register(registrationForm = form))
    }

    /** Verifies that registering an account for an existing user stores the account but leaves the user unchanged. */
    @Test
    fun registerKeepsExistingUser() = runServerTest {
        val useCases = dependency.get().useCases
        val form = registrationForm(username = "existing_user", permissions = emptyMap())
        assertEquals(expected = Outcome.Success(data = Unit), actual = useCases.userUseCases.upsertUser(user = form.user))

        assertEquals(expected = RegisterOutcome.Success, actual = useCases.authenticationUseCases.register(registrationForm = form.copy(user = form.user.copy(name = "Overwritten"))))
        assertEquals(expected = form.user, actual = useCases.userUseCases.getUser(uuid = form.user.uuid).successData())
    }

    /** Verifies that logging out with a valid refresh token succeeds and an invalid one is rejected. */
    @Test
    fun logout() = runServerTest {
        val result = seedUser(registrationForm = registrationForm(username = "logout_user", permissions = emptyMap()))
        val authenticationUseCases = dependency.get()
            .useCases
            .authenticationUseCases

        assertEquals(expected = Outcome.Rejected, actual = authenticationUseCases.logout(refreshToken = "invalid_refresh_token"))
        assertEquals(expected = Outcome.Success(data = result.user.uuid), actual = authenticationUseCases.logout(refreshToken = result.bearer.refreshToken))
    }

    /** Verifies that a disabled account cannot log in again, but may still refresh the session it already held. */
    @Test
    fun disabledAccountCannotLoginButKeepsRefreshing() = runServerTest {
        val useCases = dependency.get().useCases
        val authenticationUseCases = useCases.authenticationUseCases
        val form = registrationForm(username = "disabled_user", permissions = emptyMap())
        val result = seedUser(registrationForm = form)

        assertEquals(expected = Outcome.Success(data = Unit), actual = useCases.accountUseCases.upsertAccount(account = form.account.copy(deletedAt = now())))

        // The password still verifies, so only the disabled flag stands between the account and a brand new session.
        assertEquals(expected = LoginOutcome.Disabled, actual = authenticationUseCases.login(credentials = form.credentials))
        // The session it already had survives, since what the account may do with it is narrowed per request rather than by cutting the session.
        assertIs<Outcome.Success<BearerToken>>(value = authenticationUseCases.refreshTokens(refreshToken = result.bearer.refreshToken))
    }

    /** Verifies that refreshing the session of a disabled account never moves its expiry past [ServerConfigs.deletedAccountSessionValidity] after the deletion, and is rejected once that time is over. */
    @Test
    fun disabledAccountSessionEndsAfterDeletedAccountSessionValidity() = runServerTest {
        val useCases = dependency.get().useCases
        val authenticationUseCases = useCases.authenticationUseCases
        val validity = 60_000L
        ServerConfigs.set { it.copy(deletedAccountSessionValidity = validity) }

        suspend fun sessionExpiry(): Instant? = dependency.get().database.safeTransaction {
            UserSessionTable.selectAll()
                .where { UserSessionTable.userUuid eq FakeData.user.uuid }
                .firstOrNull()
                ?.get(UserSessionTable.expiresAt)
        }.getOrNull()

        val deletedAt = now()
        assertEquals(expected = Outcome.Success(data = Unit), actual = useCases.accountUseCases.upsertAccount(account = FakeData.account.copy(deletedAt = deletedAt)))
        val bearer = assertIs<Outcome.Success<BearerToken>>(value = authenticationUseCases.refreshTokens(refreshToken = bearerToken.refreshToken)).data
        assertEquals(expected = deletedAt + validity.milliseconds, actual = sessionExpiry())

        val expiredDeletedAt = now() - (validity * 2).milliseconds
        assertEquals(expected = Outcome.Success(data = Unit), actual = useCases.accountUseCases.upsertAccount(account = FakeData.account.copy(deletedAt = expiredDeletedAt)))
        assertEquals(expected = Outcome.Rejected, actual = authenticationUseCases.refreshTokens(refreshToken = bearer.refreshToken))
        assertEquals(expected = null, actual = sessionExpiry())
    }

    /** Verifies that refreshing tokens with a valid refresh token succeeds and an invalid one is rejected. */
    @Test
    fun refreshTokens() = runServerTest {
        val authenticationUseCases = dependency.get()
            .useCases
            .authenticationUseCases

        assertIs<Outcome.Success<BearerToken>>(value = authenticationUseCases.refreshTokens(refreshToken = bearerToken.refreshToken))
        assertEquals(expected = Outcome.Rejected, actual = authenticationUseCases.refreshTokens(refreshToken = "invalid_refresh_token"))
    }

    /** Verifies that refreshing rotates the refresh token, so the new one keeps working and, once the grace period is over, the consumed one is rejected. */
    @Test
    fun refreshTokensRotatesTheRefreshToken() = runServerTest {
        ServerConfigs.set { it.copy(refreshTokenGracePeriod = 0L) }
        val authenticationUseCases = dependency.get()
            .useCases
            .authenticationUseCases

        val bearer = assertIs<Outcome.Success<BearerToken>>(value = authenticationUseCases.refreshTokens(refreshToken = bearerToken.refreshToken)).data
        assertNotEquals(illegal = bearerToken.refreshToken, actual = bearer.refreshToken)

        assertIs<Outcome.Success<BearerToken>>(value = authenticationUseCases.refreshTokens(refreshToken = bearer.refreshToken))
        assertEquals(expected = Outcome.Rejected, actual = authenticationUseCases.refreshTokens(refreshToken = bearerToken.refreshToken))
    }

    /** Verifies that within the grace period the consumed refresh token still refreshes, rotating the session once more so the refresh token issued in between stops working. */
    @Test
    fun refreshTokensAcceptsThePreviousRefreshTokenWithinTheGracePeriod() = runServerTest {
        ServerConfigs.set { it.copy(refreshTokenGracePeriod = 60_000L) }
        val authenticationUseCases = dependency.get()
            .useCases
            .authenticationUseCases

        val rotated = assertIs<Outcome.Success<BearerToken>>(value = authenticationUseCases.refreshTokens(refreshToken = bearerToken.refreshToken)).data
        val replayed = assertIs<Outcome.Success<BearerToken>>(value = authenticationUseCases.refreshTokens(refreshToken = bearerToken.refreshToken)).data

        assertNotEquals(illegal = rotated.refreshToken, actual = replayed.refreshToken)
        assertEquals(expected = Outcome.Rejected, actual = authenticationUseCases.refreshTokens(refreshToken = rotated.refreshToken))
        assertIs<Outcome.Success<BearerToken>>(value = authenticationUseCases.refreshTokens(refreshToken = replayed.refreshToken))
    }

    /** Verifies that a previous refresh token presented after the grace period ends the session for its current refresh token too. */
    @Test
    fun reusedRefreshTokenRevokesSession() = runServerTest {
        ServerConfigs.set { it.copy(refreshTokenGracePeriod = 0L) }
        val authenticationUseCases = dependency.get()
            .useCases
            .authenticationUseCases

        val rotated = assertIs<Outcome.Success<BearerToken>>(value = authenticationUseCases.refreshTokens(refreshToken = bearerToken.refreshToken)).data
        assertEquals(expected = Outcome.Rejected, actual = authenticationUseCases.refreshTokens(refreshToken = bearerToken.refreshToken))

        assertEquals(expected = Outcome.Rejected, actual = authenticationUseCases.refreshTokens(refreshToken = rotated.refreshToken))
    }

    /** Verifies that sessions store the hash of their refresh token rather than the token itself, across login and rotation. */
    @Test
    fun refreshTokensAreStoredHashed() = runServerTest {
        val authenticationUseCases = dependency.get()
            .useCases
            .authenticationUseCases

        suspend fun storedRefreshToken(): String? = dependency.get().database.safeTransaction {
            UserSessionTable.selectAll()
                .where { UserSessionTable.userUuid eq FakeData.user.uuid }
                .firstOrNull()
                ?.get(UserSessionTable.refreshToken)
        }.getOrNull()

        assertEquals(expected = hashRefreshToken(refreshToken = bearerToken.refreshToken), actual = storedRefreshToken())

        val rotated = assertIs<Outcome.Success<BearerToken>>(value = authenticationUseCases.refreshTokens(refreshToken = bearerToken.refreshToken)).data
        assertEquals(expected = hashRefreshToken(refreshToken = rotated.refreshToken), actual = storedRefreshToken())
    }

    /** Verifies that a session can be logged out with its previous refresh token within the grace period, ending it for its current refresh token too. */
    @Test
    fun logoutAcceptsThePreviousRefreshTokenWithinTheGracePeriod() = runServerTest {
        ServerConfigs.set { it.copy(refreshTokenGracePeriod = 60_000L) }
        val authenticationUseCases = dependency.get()
            .useCases
            .authenticationUseCases

        val rotated = assertIs<Outcome.Success<BearerToken>>(value = authenticationUseCases.refreshTokens(refreshToken = bearerToken.refreshToken)).data

        assertEquals(expected = Outcome.Success(data = FakeData.user.uuid), actual = authenticationUseCases.logout(refreshToken = bearerToken.refreshToken))
        assertEquals(expected = Outcome.Rejected, actual = authenticationUseCases.refreshTokens(refreshToken = rotated.refreshToken))
    }

    /** Verifies that changing the password makes the new one log in instead of the current one, keeps the session that changed it and invalidates the other sessions of the account. */
    @Test
    fun changePassword() = runServerTest {
        val authenticationUseCases = dependency.get()
            .useCases
            .authenticationUseCases
        val form = registrationForm(username = "change_password_user", permissions = emptyMap())
        val session = seedUser(registrationForm = form)
        val otherSession = assertIs<LoginOutcome.Success>(value = authenticationUseCases.login(credentials = form.credentials)).authentication
        val newCredentials = UserCredentials(username = form.account.username, password = "new_password")

        val passwordChange = PasswordChange(refreshToken = session.bearer.refreshToken, currentPassword = form.password, newPassword = newCredentials.password)
        assertEquals(expected = PasswordChangeOutcome.Success(userUuid = form.user.uuid), actual = authenticationUseCases.changePassword(passwordChange = passwordChange))

        assertEquals(expected = LoginOutcome.Rejected, actual = authenticationUseCases.login(credentials = form.credentials))
        assertIs<LoginOutcome.Success>(value = authenticationUseCases.login(credentials = newCredentials))
        assertEquals(expected = Outcome.Rejected, actual = authenticationUseCases.refreshTokens(refreshToken = otherSession.bearer.refreshToken))
        assertIs<Outcome.Success<BearerToken>>(value = authenticationUseCases.refreshTokens(refreshToken = session.bearer.refreshToken))
    }

    /** Verifies that a password change is refused with a wrong current password or a blank, too short or too long new one, and rejected with an unknown refresh token, an expired session or a disabled account, leaving the password unchanged. */
    @Test
    fun changePasswordFailures() = runServerTest {
        val useCases = dependency.get().useCases
        val authenticationUseCases = useCases.authenticationUseCases
        val form = registrationForm(username = "change_password_failures_user", permissions = emptyMap())
        val session = seedUser(registrationForm = form)
        val passwordChange = PasswordChange(refreshToken = session.bearer.refreshToken, currentPassword = form.password, newPassword = "new_password")

        val maxAttempts = ServerConfigs.value.maxPasswordAttempts
        assertEquals(
            expected = PasswordChangeOutcome.InvalidPassword(failure = PasswordFailure.WrongPassword(remainingAttempts = maxAttempts - 1)),
            actual = authenticationUseCases.changePassword(passwordChange = passwordChange.copy(currentPassword = "wrong_password"))
        )
        val minLength = ServerConfigs.value.minPasswordLength
        listOf(" ", "a".repeat(n = minLength - 1)).forEach { newPassword ->
            assertEquals(expected = PasswordChangeOutcome.InvalidPassword(failure = PasswordFailure.TooShort(minLength = minLength)), actual = authenticationUseCases.changePassword(passwordChange = passwordChange.copy(newPassword = newPassword)))
        }
        assertEquals(expected = PasswordChangeOutcome.InvalidPassword(failure = PasswordFailure.TooLong), actual = authenticationUseCases.changePassword(passwordChange = passwordChange.copy(newPassword = "a".repeat(n = 73))))
        assertEquals(expected = PasswordChangeOutcome.Rejected, actual = authenticationUseCases.changePassword(passwordChange = passwordChange.copy(refreshToken = "invalid_refresh_token")))

        ServerConfigs.set { it.copy(refreshTokenValidity = -1_000L) }
        val expiredSession = assertIs<LoginOutcome.Success>(value = authenticationUseCases.login(credentials = form.credentials)).authentication
        assertEquals(expected = PasswordChangeOutcome.Rejected, actual = authenticationUseCases.changePassword(passwordChange = passwordChange.copy(refreshToken = expiredSession.bearer.refreshToken)))

        assertEquals(expected = Outcome.Success(data = Unit), actual = useCases.accountUseCases.upsertAccount(account = form.account.copy(deletedAt = now())))
        assertEquals(expected = PasswordChangeOutcome.Rejected, actual = authenticationUseCases.changePassword(passwordChange = passwordChange))

        assertEquals(expected = Outcome.Success(data = Unit), actual = useCases.accountUseCases.upsertAccount(account = form.account))
        assertIs<LoginOutcome.Success>(value = authenticationUseCases.login(credentials = form.credentials))
    }

    /** Verifies that a session is ended by its last allowed wrong current password, that each earlier one reports the attempts left, that blank new passwords are not counted, and that the account's other sessions and its password are left as they were. */
    @Test
    fun changePasswordEndsSessionAfterMaxAttempts() = runServerTest {
        ServerConfigs.set { it.copy(maxPasswordAttempts = 3) }
        val authenticationUseCases = dependency.get().useCases.authenticationUseCases
        val form = registrationForm(username = "change_password_attempts_user", permissions = emptyMap())
        val session = seedUser(registrationForm = form)
        val otherSession = assertIs<LoginOutcome.Success>(value = authenticationUseCases.login(credentials = form.credentials)).authentication
        val passwordChange = PasswordChange(refreshToken = session.bearer.refreshToken, currentPassword = "wrong_password", newPassword = "new_password")

        val tooShort = PasswordChangeOutcome.InvalidPassword(failure = PasswordFailure.TooShort(minLength = ServerConfigs.value.minPasswordLength))
        repeat(times = 5) { assertEquals(expected = tooShort, actual = authenticationUseCases.changePassword(passwordChange = passwordChange.copy(newPassword = " "))) }
        listOf(2, 1).forEach { remainingAttempts ->
            assertEquals(
                expected = PasswordChangeOutcome.InvalidPassword(failure = PasswordFailure.WrongPassword(remainingAttempts = remainingAttempts)),
                actual = authenticationUseCases.changePassword(passwordChange = passwordChange)
            )
        }
        assertEquals(expected = PasswordChangeOutcome.Rejected, actual = authenticationUseCases.changePassword(passwordChange = passwordChange))

        assertEquals(expected = PasswordChangeOutcome.Rejected, actual = authenticationUseCases.changePassword(passwordChange = passwordChange.copy(currentPassword = form.password)))
        assertEquals(expected = Outcome.Rejected, actual = authenticationUseCases.refreshTokens(refreshToken = session.bearer.refreshToken))
        assertIs<Outcome.Success<BearerToken>>(value = authenticationUseCases.refreshTokens(refreshToken = otherSession.bearer.refreshToken))
        assertIs<LoginOutcome.Success>(value = authenticationUseCases.login(credentials = form.credentials))
    }

    /** Verifies that a username, known or not, is locked once its failed logins reach the maximum, refusing even the right password, and is unlocked once the lock duration passes. */
    @Test
    fun loginLocksAfterMaxAttempts() = runServerTest {
        ServerConfigs.set { it.copy(maxLoginAttempts = 3, loginLockDuration = 60_000L) }
        val authenticationUseCases = dependency.get().useCases.authenticationUseCases
        val wrong = UserCredentials(username = FakeData.credentials.username, password = "wrong_password")
        val unknown = UserCredentials(username = "unknown_user", password = "wrong_password")

        listOf(wrong, unknown).forEach { credentials ->
            repeat(times = 3) { assertEquals(expected = LoginOutcome.Rejected, actual = authenticationUseCases.login(credentials = credentials)) }
            assertEquals(expected = LoginOutcome.Locked, actual = authenticationUseCases.login(credentials = credentials))
        }
        assertEquals(expected = LoginOutcome.Locked, actual = authenticationUseCases.login(credentials = FakeData.credentials))

        ServerConfigs.set { it.copy(loginLockDuration = 0L) }
        assertIs<LoginOutcome.Success>(value = authenticationUseCases.login(credentials = FakeData.credentials))
    }

    /** Verifies that a successful login clears the failed logins counted for its username. */
    @Test
    fun loginSuccessClearsFailures() = runServerTest {
        ServerConfigs.set { it.copy(maxLoginAttempts = 3, loginLockDuration = 60_000L) }
        val authenticationUseCases = dependency.get().useCases.authenticationUseCases
        val wrong = UserCredentials(username = FakeData.credentials.username, password = "wrong_password")

        repeat(times = 2) { assertEquals(expected = LoginOutcome.Rejected, actual = authenticationUseCases.login(credentials = wrong)) }
        assertIs<LoginOutcome.Success>(value = authenticationUseCases.login(credentials = FakeData.credentials))
        repeat(times = 2) { assertEquals(expected = LoginOutcome.Rejected, actual = authenticationUseCases.login(credentials = wrong)) }
        assertIs<LoginOutcome.Success>(value = authenticationUseCases.login(credentials = FakeData.credentials))
    }

    /** Verifies that a successful password change clears the wrong current passwords counted for the session. */
    @Test
    fun changePasswordSuccessResetsAttempts() = runServerTest {
        ServerConfigs.set { it.copy(maxPasswordAttempts = 2) }
        val authenticationUseCases = dependency.get().useCases.authenticationUseCases
        val form = registrationForm(username = "change_password_reset_user", permissions = emptyMap())
        val session = seedUser(registrationForm = form)
        val wrong = PasswordChange(refreshToken = session.bearer.refreshToken, currentPassword = "wrong_password", newPassword = "new_password")

        val wrongPassword = PasswordChangeOutcome.InvalidPassword(failure = PasswordFailure.WrongPassword(remainingAttempts = 1))
        assertEquals(expected = wrongPassword, actual = authenticationUseCases.changePassword(passwordChange = wrong))
        assertEquals(expected = PasswordChangeOutcome.Success(userUuid = form.user.uuid), actual = authenticationUseCases.changePassword(passwordChange = wrong.copy(currentPassword = form.password)))
        assertEquals(expected = wrongPassword, actual = authenticationUseCases.changePassword(passwordChange = wrong))
        assertEquals(expected = PasswordChangeOutcome.Rejected, actual = authenticationUseCases.changePassword(passwordChange = wrong))
    }


    @Test
    fun databaseFailureIsAnErrorNotARejection() = runServerTest {
        val authenticationUseCases = dependency.get()
            .useCases
            .authenticationUseCases
        dependency.get().database.reset()

        assertEquals(expected = LoginOutcome.Error, actual = authenticationUseCases.login(credentials = FakeData.credentials))
        assertEquals(expected = Outcome.Error, actual = authenticationUseCases.refreshTokens(refreshToken = bearerToken.refreshToken))
        assertEquals(expected = Outcome.Error, actual = authenticationUseCases.logout(refreshToken = bearerToken.refreshToken))
        assertEquals(
            expected = PasswordChangeOutcome.Error,
            actual = authenticationUseCases.changePassword(passwordChange = PasswordChange(refreshToken = bearerToken.refreshToken, currentPassword = FakeData.credentials.password, newPassword = "new_password"))
        )
    }

    /** Verifies that a refreshed access token carries the account's current stored permissions and deleted state rather than those it was first issued with. */
    @Test
    fun refreshTokensReflectsCurrentAccess() = runServerTest {
        val useCases = dependency.get().useCases
        val authenticationUseCases = useCases.authenticationUseCases

        val updatedAccount = FakeData.account.copy(permissions = mapOf(EntityType.TASK to Permission.WRITE), deletedAt = now())
        assertEquals(expected = Outcome.Success(data = Unit), actual = useCases.accountUseCases.upsertAccount(account = updatedAccount))

        val bearer = assertIs<Outcome.Success<BearerToken>>(value = authenticationUseCases.refreshTokens(refreshToken = bearerToken.refreshToken)).data
        val payload = createVerifier().verify(bearer.accessToken)
        assertEquals(expected = FakeData.user.uuid.toString(), actual = payload.subject)
        assertEquals(expected = UserAccess(permissions = updatedAccount.permissions, deletedAt = updatedAccount.deletedAt), actual = payload.getAccess())
    }
}
