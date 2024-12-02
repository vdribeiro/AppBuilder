package com.app.builder.domain.route.provider

import kotlin.test.Test
import kotlin.test.assertEquals
import io.ktor.client.call.body
import io.ktor.http.HttpStatusCode
import com.app.builder.core.config.ServerConfigs
import com.app.builder.core.security.uuid
import com.app.builder.data.http.HttpRequest
import com.app.builder.data.http.URL
import com.app.builder.data.http.post
import com.app.builder.domain.Authentication
import com.app.builder.domain.RegistrationForm
import com.app.builder.domain.UserCredentials
import com.app.builder.test.FakeData
import com.app.builder.test.TestCase

class AuthenticationTest: TestCase() {

    /** Verifies that registering with a valid form succeeds and rejects malformed requests. */
    @Test
    fun register() = runServerTest {
        installRouting()
        val client = createClient(token = adminBearerToken.accessToken)

        assertEquals(expected = HttpStatusCode.BadRequest, actual = client.post(request = HttpRequest(url = URL.Register), body = "No form").status)
        val response = client.post(request = HttpRequest(url = URL.Register), body = FakeData.registrationForm)
        assertEquals(expected = HttpStatusCode.OK, actual = response.status)
        assertEquals(expected = FakeData.user, actual = response.body<Authentication>().user)
    }

    /** Verifies that registering without session permission is forbidden. */
    @Test
    fun registerWithoutSessionPermission() = runServerTest {
        installRouting()
        val client = createClient(token = bearerToken.accessToken)

        val response = client.post(request = HttpRequest(url = URL.Register), body = FakeData.registrationForm)
        assertEquals(expected = HttpStatusCode.Forbidden, actual = response.status)
    }

    /** Verifies that logging in with valid credentials succeeds and rejects malformed requests. */
    @Test
    fun login() = runServerTest {
        installRouting()
        val client = createClient(token = null)

        assertEquals(expected = HttpStatusCode.BadRequest, actual = client.post(request = HttpRequest(url = URL.Login), body = "No credentials").status)
        val response = client.post(
            request = HttpRequest(url = URL.Login),
            body = FakeData.adminCredentials
        )
        assertEquals(expected = HttpStatusCode.OK, actual = response.status)
        assertEquals(expected = FakeData.adminUser, actual = response.body<Authentication>().user)
    }

    /** Verifies that logging out rejects an invalid refresh token and succeeds with a valid one. */
    @Test
    fun logout() = runServerTest {
        installRouting()
        val client = createClient(token = adminBearerToken.accessToken)
        val refreshToken = adminBearerToken.refreshToken

        assertEquals(expected = HttpStatusCode.Unauthorized, actual = client.post(request = HttpRequest(url = URL.Logout), body = "invalid_refresh_token").status)
        assertEquals(expected = HttpStatusCode.OK, actual = client.post(request = HttpRequest(url = URL.Logout), body = refreshToken).status)
    }

    /** Verifies that refreshing tokens rejects an invalid refresh token and succeeds with a valid one. */
    @Test
    fun refreshToken() = runServerTest {
        installRouting()
        val client = createClient(token = adminBearerToken.accessToken)
        val refreshToken = adminBearerToken.refreshToken

        assertEquals(expected = HttpStatusCode.Unauthorized, actual = client.post(request = HttpRequest(url = URL.RefreshTokens), body = "invalid_refresh_token").status)
        assertEquals(expected = HttpStatusCode.OK, actual = client.post(request = HttpRequest(url = URL.RefreshTokens), body = refreshToken).status)
    }

    /** Verifies that refreshing tokens with an existing but expired session deletes it and rejects the request. */
    @Test
    fun refreshTokenWithExpiredSession() = runServerTest {
        installRouting()
        ServerConfigs.set { it.copy(refreshTokenValidity = -1_000L) }
        val expiredAuth = seedUser(
            registrationForm = RegistrationForm(
                user = FakeData.user.copy(uuid = uuid()),
                credentials = UserCredentials(username = "expired_session_user", password = "pass123")
            )
        )

        val client = createClient(token = null)
        assertEquals(expected = HttpStatusCode.Unauthorized, actual = client.post(request = HttpRequest(url = URL.RefreshTokens), body = expiredAuth.bearer.refreshToken).status)
    }

    /** Verifies that a refresh token consumed by logout can no longer be used to refresh tokens. */
    @Test
    fun refreshTokenAfterLogoutFails() = runServerTest {
        installRouting()
        val client = createClient(token = adminBearerToken.accessToken)
        val refreshToken = adminBearerToken.refreshToken

        assertEquals(expected = HttpStatusCode.OK, actual = client.post(request = HttpRequest(url = URL.Logout), body = refreshToken).status)
        assertEquals(expected = HttpStatusCode.Unauthorized, actual = client.post(request = HttpRequest(url = URL.RefreshTokens), body = refreshToken).status)
    }
}