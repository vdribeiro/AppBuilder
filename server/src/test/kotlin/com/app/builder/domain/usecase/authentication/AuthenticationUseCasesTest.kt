package com.app.builder.domain.usecase.authentication

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import com.app.builder.data.database.create
import com.app.builder.data.database.reset
import com.app.builder.domain.UserCredentials
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
}
