package com.app.builder.domain.usecase.authentication

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import com.app.builder.core.locale.now
import com.app.builder.core.security.createVerifier
import com.app.builder.data.database.create
import com.app.builder.data.database.reset
import com.app.builder.domain.EntityType
import com.app.builder.domain.Permission
import com.app.builder.domain.UserCredentials
import com.app.builder.domain.permission.UserAccess
import com.app.builder.domain.permission.getAccess
import com.app.builder.test.FakeData
import com.app.builder.test.TestCase

class AuthenticationUseCasesTest: TestCase() {

    /** Verifies that a registered user can log in with correct credentials and is rejected with wrong ones. */
    @Test
    fun registerLogin() = runServerTest {
        dependency.get().database.reset()
        dependency.get().database.create()

        val authenticationUseCases = dependency.get()
            .useCases
            .authenticationUseCases

        assertNull(actual = authenticationUseCases.login(credentials = FakeData.credentials))

        val registerResult = authenticationUseCases.register(registrationForm = FakeData.registrationForm)
        assertNotNull(actual = registerResult)
        assertEquals(expected = FakeData.user, actual = registerResult.user)

        val loginResult = authenticationUseCases.login(credentials = FakeData.credentials)
        assertNotNull(actual = loginResult)
        assertEquals(expected = FakeData.authentication.user, actual = loginResult.user)

        assertNull(actual = authenticationUseCases.login(credentials = UserCredentials(username = FakeData.credentials.username, password = "wrong_password")))
    }

    /** Verifies that logging out with a valid refresh token succeeds and an invalid one is rejected. */
    @Test
    fun logout() = runServerTest {
        val authenticationUseCases = dependency.get()
            .useCases
            .authenticationUseCases

        val result = authenticationUseCases.register(registrationForm = FakeData.registrationForm)
        assertNotNull(actual = result)

        assertNull(actual = authenticationUseCases.logout(refreshToken = "invalid_refresh_token"))
        assertEquals(expected = result.user.uuid, actual = authenticationUseCases.logout(refreshToken = result.bearer.refreshToken))
    }

    /** Verifies that a deleted user cannot log in again, but may still refresh the session it already held, so a device that was offline at deletion can come back and drain its queue however long that takes. */
    @Test
    fun deletedUserCannotLoginButKeepsRefreshing() = runServerTest {
        val useCases = dependency.get().useCases
        val authenticationUseCases = useCases.authenticationUseCases

        val result = authenticationUseCases.register(registrationForm = FakeData.registrationForm)
        assertNotNull(actual = result)

        assertTrue(actual = useCases.userUseCases.upsertUser(user = FakeData.user.copy(deletedAt = now())))

        // The password still verifies, so only the deleted flag stands between the account and a brand new session.
        assertNull(actual = authenticationUseCases.login(credentials = FakeData.credentials))
        // The session it already had survives, since what the account may do with it is narrowed per request rather than by cutting the session.
        assertNotNull(actual = authenticationUseCases.refreshTokens(refreshToken = result.bearer.refreshToken))
    }

    /** Verifies that refreshing tokens with a valid refresh token succeeds and an invalid one is rejected. */
    @Test
    fun refreshTokens() = runServerTest {
        val authenticationUseCases = dependency.get()
            .useCases
            .authenticationUseCases

        val result = authenticationUseCases.register(registrationForm = FakeData.registrationForm)
        assertNotNull(actual = result)

        val bearer = authenticationUseCases.refreshTokens(refreshToken = result.bearer.refreshToken)
        assertNotNull(actual = bearer)

        assertNull(actual = authenticationUseCases.refreshTokens(refreshToken = "invalid_refresh_token"))
    }

    /** Verifies that refreshing rotates the refresh token, so the consumed one is rejected and only the new one keeps working. */
    @Test
    fun refreshTokensRotatesTheRefreshToken() = runServerTest {
        val authenticationUseCases = dependency.get()
            .useCases
            .authenticationUseCases

        val result = authenticationUseCases.register(registrationForm = FakeData.registrationForm)
        assertNotNull(actual = result)

        val bearer = assertNotNull(actual = authenticationUseCases.refreshTokens(refreshToken = result.bearer.refreshToken))
        assertNotEquals(illegal = result.bearer.refreshToken, actual = bearer.refreshToken)

        assertNull(actual = authenticationUseCases.refreshTokens(refreshToken = result.bearer.refreshToken))
        assertNotNull(actual = authenticationUseCases.refreshTokens(refreshToken = bearer.refreshToken))
    }

    /** Verifies that a refreshed access token carries the user's current stored permissions and deleted state rather than those it was first issued with. */
    @Test
    fun refreshTokensReflectsCurrentAccess() = runServerTest {
        val useCases = dependency.get().useCases
        val authenticationUseCases = useCases.authenticationUseCases

        val result = authenticationUseCases.register(registrationForm = FakeData.registrationForm)
        assertNotNull(actual = result)

        val updatedUser = FakeData.user.copy(permissions = mapOf(EntityType.TASK to Permission.WRITE), deletedAt = now())
        assertTrue(actual = useCases.userUseCases.upsertUser(user = updatedUser))

        val bearer = assertNotNull(actual = authenticationUseCases.refreshTokens(refreshToken = result.bearer.refreshToken))
        val payload = createVerifier().verify(bearer.accessToken)
        assertEquals(expected = FakeData.user.uuid.toString(), actual = payload.subject)
        assertEquals(expected = UserAccess(permissions = updatedUser.permissions, deletedAt = updatedUser.deletedAt), actual = payload.getAccess())
    }
}
