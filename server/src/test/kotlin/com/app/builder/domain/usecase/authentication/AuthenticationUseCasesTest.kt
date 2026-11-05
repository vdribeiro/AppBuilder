package com.app.builder.domain.usecase.authentication

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import com.app.builder.core.locale.now
import com.app.builder.core.security.createVerifier
import com.app.builder.core.security.uuid
import com.app.builder.data.database.create
import com.app.builder.data.database.reset
import com.app.builder.domain.EntityType
import com.app.builder.domain.Permission
import com.app.builder.domain.UserCredentials
import com.app.builder.domain.permission.UserAccess
import com.app.builder.domain.permission.getAccess
import com.app.builder.domain.usecase.WriteResult
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

        assertNull(actual = authenticationUseCases.login(credentials = FakeData.credentials))

        val registerResult = authenticationUseCases.register(registrationForm = FakeData.registrationForm)
        assertEquals(expected = WriteResult.SUCCESS, actual = registerResult)

        val loginResult = authenticationUseCases.login(credentials = FakeData.credentials)
        assertNotNull(actual = loginResult)
        assertEquals(expected = FakeData.user, actual = loginResult.user)
        assertEquals(expected = FakeData.account, actual = loginResult.account)

        assertNull(actual = authenticationUseCases.login(credentials = UserCredentials(username = FakeData.credentials.username, password = "wrong_password")))
    }

    /** Verifies that registering is rejected when the user already has an account, the username is taken, or the account belongs to another user. */
    @Test
    fun registerConflicts() = runServerTest {
        val authenticationUseCases = dependency.get()
            .useCases
            .authenticationUseCases

        assertEquals(expected = WriteResult.CONFLICT, actual = authenticationUseCases.register(registrationForm = FakeData.registrationForm.copy(account = FakeData.account.copy(username = "another_username"))))

        val form = registrationForm(username = FakeData.account.username, permissions = emptyMap())
        assertEquals(expected = WriteResult.CONFLICT, actual = authenticationUseCases.register(registrationForm = form))
        assertEquals(expected = WriteResult.INVALID, actual = authenticationUseCases.register(registrationForm = form.copy(account = form.account.copy(userUuid = uuid(), username = "new_username"))))
        assertNull(actual = dependency.get().useCases.userUseCases.getUser(uuid = form.user.uuid))
    }

    /** Verifies that registering an account for an existing user stores the account but leaves the user unchanged. */
    @Test
    fun registerKeepsExistingUser() = runServerTest {
        val useCases = dependency.get().useCases
        val form = registrationForm(username = "existing_user", permissions = emptyMap())
        assertTrue(actual = useCases.userUseCases.upsertUser(user = form.user))

        assertEquals(expected = WriteResult.SUCCESS, actual = useCases.authenticationUseCases.register(registrationForm = form.copy(user = form.user.copy(name = "Overwritten"))))
        assertEquals(expected = form.user, actual = useCases.userUseCases.getUser(uuid = form.user.uuid))
    }

    /** Verifies that logging out with a valid refresh token succeeds and an invalid one is rejected. */
    @Test
    fun logout() = runServerTest {
        val result = seedUser(registrationForm = registrationForm(username = "logout_user", permissions = emptyMap()))
        val authenticationUseCases = dependency.get()
            .useCases
            .authenticationUseCases

        assertNull(actual = authenticationUseCases.logout(refreshToken = "invalid_refresh_token"))
        assertEquals(expected = result.user.uuid, actual = authenticationUseCases.logout(refreshToken = result.bearer.refreshToken))
    }

    /** Verifies that a disabled account cannot log in again, but may still refresh the session it already held, so a device that was offline when the account was disabled can come back and drain its queue however long that takes. */
    @Test
    fun disabledAccountCannotLoginButKeepsRefreshing() = runServerTest {
        val useCases = dependency.get().useCases
        val authenticationUseCases = useCases.authenticationUseCases
        val form = registrationForm(username = "disabled_user", permissions = emptyMap())
        val result = seedUser(registrationForm = form)

        assertEquals(expected = WriteResult.SUCCESS, actual = useCases.accountUseCases.upsertAccount(account = form.account.copy(deletedAt = now())))

        // The password still verifies, so only the disabled flag stands between the account and a brand new session.
        assertNull(actual = authenticationUseCases.login(credentials = form.credentials))
        // The session it already had survives, since what the account may do with it is narrowed per request rather than by cutting the session.
        assertNotNull(actual = authenticationUseCases.refreshTokens(refreshToken = result.bearer.refreshToken))
    }

    /** Verifies that refreshing tokens with a valid refresh token succeeds and an invalid one is rejected. */
    @Test
    fun refreshTokens() = runServerTest {
        val authenticationUseCases = dependency.get()
            .useCases
            .authenticationUseCases

        assertNotNull(actual = authenticationUseCases.refreshTokens(refreshToken = bearerToken.refreshToken))
        assertNull(actual = authenticationUseCases.refreshTokens(refreshToken = "invalid_refresh_token"))
    }

    /** Verifies that refreshing rotates the refresh token, so the consumed one is rejected and only the new one keeps working. */
    @Test
    fun refreshTokensRotatesTheRefreshToken() = runServerTest {
        val authenticationUseCases = dependency.get()
            .useCases
            .authenticationUseCases

        val bearer = assertNotNull(actual = authenticationUseCases.refreshTokens(refreshToken = bearerToken.refreshToken))
        assertNotEquals(illegal = bearerToken.refreshToken, actual = bearer.refreshToken)

        assertNull(actual = authenticationUseCases.refreshTokens(refreshToken = bearerToken.refreshToken))
        assertNotNull(actual = authenticationUseCases.refreshTokens(refreshToken = bearer.refreshToken))
    }

    /** Verifies that a refreshed access token carries the account's current stored permissions and deleted state rather than those it was first issued with. */
    @Test
    fun refreshTokensReflectsCurrentAccess() = runServerTest {
        val useCases = dependency.get().useCases
        val authenticationUseCases = useCases.authenticationUseCases

        val updatedAccount = FakeData.account.copy(permissions = mapOf(EntityType.TASK to Permission.WRITE), deletedAt = now())
        assertEquals(expected = WriteResult.SUCCESS, actual = useCases.accountUseCases.upsertAccount(account = updatedAccount))

        val bearer = assertNotNull(actual = authenticationUseCases.refreshTokens(refreshToken = bearerToken.refreshToken))
        val payload = createVerifier().verify(bearer.accessToken)
        assertEquals(expected = FakeData.user.uuid.toString(), actual = payload.subject)
        assertEquals(expected = UserAccess(permissions = updatedAccount.permissions, deletedAt = updatedAccount.deletedAt), actual = payload.getAccess())
    }
}
