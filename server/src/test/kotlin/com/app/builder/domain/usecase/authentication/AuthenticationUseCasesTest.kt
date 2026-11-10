package com.app.builder.domain.usecase.authentication

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import com.app.builder.core.config.ServerConfigs
import com.app.builder.core.locale.now
import com.app.builder.core.security.createVerifier
import com.app.builder.core.security.uuid
import com.app.builder.data.database.create
import com.app.builder.data.database.reset
import com.app.builder.domain.Authentication
import com.app.builder.domain.BearerToken
import com.app.builder.domain.EntityType
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

        assertEquals(expected = Outcome.Rejected, actual = authenticationUseCases.login(credentials = FakeData.credentials))

        val registerResult = authenticationUseCases.register(registrationForm = FakeData.registrationForm)
        assertEquals(expected = Outcome.Success(data = Unit), actual = registerResult)

        val loginResult = assertIs<Outcome.Success<Authentication>>(value = authenticationUseCases.login(credentials = FakeData.credentials)).data
        assertEquals(expected = FakeData.user, actual = loginResult.user)
        assertEquals(expected = FakeData.account, actual = loginResult.account)

        assertEquals(expected = Outcome.Rejected, actual = authenticationUseCases.login(credentials = UserCredentials(username = FakeData.credentials.username, password = "wrong_password")))
    }

    /** Verifies that registering is rejected when the user already has an account, the username is taken, or the account belongs to another user. */
    @Test
    fun registerConflicts() = runServerTest {
        val authenticationUseCases = dependency.get()
            .useCases
            .authenticationUseCases

        assertEquals(expected = Outcome.Conflict, actual = authenticationUseCases.register(registrationForm = FakeData.registrationForm.copy(account = FakeData.account.copy(username = "another_username"))))

        val form = registrationForm(username = FakeData.account.username, permissions = emptyMap())
        assertEquals(expected = Outcome.Conflict, actual = authenticationUseCases.register(registrationForm = form))
        assertEquals(expected = Outcome.Invalid, actual = authenticationUseCases.register(registrationForm = form.copy(account = form.account.copy(userUuid = uuid(), username = "new_username"))))
        assertNull(actual = dependency.get().useCases.userUseCases.getUser(uuid = form.user.uuid))
    }

    /** Verifies that registering an account for an existing user stores the account but leaves the user unchanged. */
    @Test
    fun registerKeepsExistingUser() = runServerTest {
        val useCases = dependency.get().useCases
        val form = registrationForm(username = "existing_user", permissions = emptyMap())
        assertTrue(actual = useCases.userUseCases.upsertUser(user = form.user))

        assertEquals(expected = Outcome.Success(data = Unit), actual = useCases.authenticationUseCases.register(registrationForm = form.copy(user = form.user.copy(name = "Overwritten"))))
        assertEquals(expected = form.user, actual = useCases.userUseCases.getUser(uuid = form.user.uuid))
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

    /** Verifies that a disabled account cannot log in again, but may still refresh the session it already held, so a device that was offline when the account was disabled can come back and drain its queue however long that takes. */
    @Test
    fun disabledAccountCannotLoginButKeepsRefreshing() = runServerTest {
        val useCases = dependency.get().useCases
        val authenticationUseCases = useCases.authenticationUseCases
        val form = registrationForm(username = "disabled_user", permissions = emptyMap())
        val result = seedUser(registrationForm = form)

        assertEquals(expected = Outcome.Success(data = Unit), actual = useCases.accountUseCases.upsertAccount(account = form.account.copy(deletedAt = now())))

        // The password still verifies, so only the disabled flag stands between the account and a brand new session.
        assertEquals(expected = Outcome.Rejected, actual = authenticationUseCases.login(credentials = form.credentials))
        // The session it already had survives, since what the account may do with it is narrowed per request rather than by cutting the session.
        assertIs<Outcome.Success<BearerToken>>(value = authenticationUseCases.refreshTokens(refreshToken = result.bearer.refreshToken))
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

    /** Verifies that refreshing rotates the refresh token, so once the grace period is over the consumed one is rejected and only the new one keeps working. */
    @Test
    fun refreshTokensRotatesTheRefreshToken() = runServerTest {
        ServerConfigs.set { it.copy(refreshTokenGracePeriod = 0L) }
        val authenticationUseCases = dependency.get()
            .useCases
            .authenticationUseCases

        val bearer = assertIs<Outcome.Success<BearerToken>>(value = authenticationUseCases.refreshTokens(refreshToken = bearerToken.refreshToken)).data
        assertNotEquals(illegal = bearerToken.refreshToken, actual = bearer.refreshToken)

        assertEquals(expected = Outcome.Rejected, actual = authenticationUseCases.refreshTokens(refreshToken = bearerToken.refreshToken))
        assertIs<Outcome.Success<BearerToken>>(value = authenticationUseCases.refreshTokens(refreshToken = bearer.refreshToken))
    }

    /** Verifies that within the grace period the consumed refresh token still refreshes, handing back the session's current refresh token without rotating it again. */
    @Test
    fun refreshTokensAcceptsThePreviousRefreshTokenWithinTheGracePeriod() = runServerTest {
        ServerConfigs.set { it.copy(refreshTokenGracePeriod = 60_000L) }
        val authenticationUseCases = dependency.get()
            .useCases
            .authenticationUseCases

        val rotated = assertIs<Outcome.Success<BearerToken>>(value = authenticationUseCases.refreshTokens(refreshToken = bearerToken.refreshToken)).data
        val replayed = assertIs<Outcome.Success<BearerToken>>(value = authenticationUseCases.refreshTokens(refreshToken = bearerToken.refreshToken)).data

        assertEquals(expected = rotated.refreshToken, actual = replayed.refreshToken)
        assertIs<Outcome.Success<BearerToken>>(value = authenticationUseCases.refreshTokens(refreshToken = rotated.refreshToken))
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

    /** Verifies that a failure on the server's side is reported as an error rather than as a rejected credential. */
    @Test
    fun databaseFailureIsAnErrorNotARejection() = runServerTest {
        val authenticationUseCases = dependency.get()
            .useCases
            .authenticationUseCases
        dependency.get().database.reset()

        assertEquals(expected = Outcome.Error, actual = authenticationUseCases.login(credentials = FakeData.credentials))
        assertEquals(expected = Outcome.Error, actual = authenticationUseCases.refreshTokens(refreshToken = bearerToken.refreshToken))
        assertEquals(expected = Outcome.Error, actual = authenticationUseCases.logout(refreshToken = bearerToken.refreshToken))
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
