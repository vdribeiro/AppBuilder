package com.app.builder.domain.route.provider

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import io.ktor.client.call.body
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import com.app.builder.core.config.ServerConfigs
import com.app.builder.core.locale.now
import com.app.builder.core.security.uuid
import com.app.builder.data.http.HttpRequest
import com.app.builder.data.http.URL
import com.app.builder.data.http.post
import com.app.builder.domain.Account
import com.app.builder.domain.Authentication
import com.app.builder.domain.EntityType
import com.app.builder.domain.Permission
import com.app.builder.domain.User
import com.app.builder.domain.UserCredentials
import com.app.builder.test.FakeData
import com.app.builder.test.TestCase
import io.ktor.client.request.get as ktorGet

class AuthenticationTest: TestCase() {

    /** Verifies that registering with a valid form responds with the created account and no session tokens, and rejects malformed requests. */
    @Test
    fun register() = runServerTest {
        installRouting()
        val client = createClient(token = adminBearerToken.accessToken)
        val form = registrationForm(username = "new_user", permissions = mapOf(EntityType.TASK to Permission.READ))

        assertEquals(expected = HttpStatusCode.BadRequest, actual = client.post(request = HttpRequest(url = URL.Register), body = "No form").status)
        val response = client.post(request = HttpRequest(url = URL.Register), body = form)
        assertEquals(expected = HttpStatusCode.OK, actual = response.status)
        assertEquals(expected = form.account, actual = response.body<Account>())
        assertFalse(actual = response.bodyAsText().contains(other = "accessToken"))
        assertFalse(actual = response.bodyAsText().contains(other = "refreshToken"))

        val user = client.ktorGet(urlString = "${URL.Users.path}/${form.user.uuid}")
        assertEquals(expected = HttpStatusCode.OK, actual = user.status)
        assertEquals(expected = form.user, actual = user.body<User>())
        assertEquals(expected = HttpStatusCode.OK, actual = createClient(token = null).post(request = HttpRequest(url = URL.Login), body = form.credentials).status)
    }

    /** Verifies that registering an account whose user uuid differs from the form's user is rejected. */
    @Test
    fun registerWithMismatchedUser() = runServerTest {
        installRouting()
        val client = createClient(token = adminBearerToken.accessToken)
        val form = registrationForm(username = "mismatched_user", permissions = emptyMap())

        val response = client.post(request = HttpRequest(url = URL.Register), body = form.copy(account = form.account.copy(userUuid = uuid())))
        assertEquals(expected = HttpStatusCode.BadRequest, actual = response.status)
    }

    /** Verifies that registering is rejected when the user already has an account or the username is taken. */
    @Test
    fun registerConflicts() = runServerTest {
        installRouting()
        val client = createClient(token = adminBearerToken.accessToken)

        assertEquals(expected = HttpStatusCode.Conflict, actual = client.post(request = HttpRequest(url = URL.Register), body = FakeData.registrationForm).status)
        val takenUsername = registrationForm(username = FakeData.account.username, permissions = emptyMap())
        assertEquals(expected = HttpStatusCode.Conflict, actual = client.post(request = HttpRequest(url = URL.Register), body = takenUsername).status)
    }

    /** Verifies that registering an account for an existing user leaves the stored user unchanged. */
    @Test
    fun registerKeepsExistingUser() = runServerTest {
        installRouting()
        val client = createClient(token = adminBearerToken.accessToken)
        val form = registrationForm(username = "existing_user", permissions = emptyMap())
        val existingUser = form.user.copy(name = "Existing")
        assertEquals(expected = HttpStatusCode.OK, actual = client.post(request = HttpRequest(url = URL.Users), body = existingUser).status)

        val response = client.post(request = HttpRequest(url = URL.Register), body = form.copy(user = form.user.copy(name = "Overwritten")))
        assertEquals(expected = HttpStatusCode.OK, actual = response.status)
        assertEquals(expected = existingUser, actual = client.ktorGet(urlString = "${URL.Users.path}/${form.user.uuid}").body<User>())
    }

    /** Verifies that registering requires the account write permission, even for a user with the user write permission. */
    @Test
    fun registerWithoutAccountWritePermission() = runServerTest {
        installRouting()
        val form = registrationForm(username = "another_user", permissions = emptyMap())

        // FakeData.account only has ACCOUNT -> READ.
        val client = createClient(token = bearerToken.accessToken)
        assertEquals(expected = HttpStatusCode.Forbidden, actual = client.post(request = HttpRequest(url = URL.Register), body = form).status)

        val userWriter = seedUser(registrationForm = registrationForm(username = "user_writer", permissions = mapOf(EntityType.USER to Permission.WRITE)))
        val userWriterClient = createClient(token = userWriter.bearer.accessToken)
        assertEquals(expected = HttpStatusCode.Forbidden, actual = userWriterClient.post(request = HttpRequest(url = URL.Register), body = form).status)
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
        val authentication = response.body<Authentication>()
        assertEquals(expected = FakeData.adminUser, actual = authentication.user)
        assertEquals(expected = FakeData.adminAccount, actual = authentication.account)
    }

    /** Verifies that logging in with a disabled account is rejected. */
    @Test
    fun loginWithDisabledAccount() = runServerTest {
        installRouting()
        val adminClient = createClient(token = adminBearerToken.accessToken)
        assertEquals(expected = HttpStatusCode.OK, actual = adminClient.post(request = HttpRequest(url = URL.Accounts), body = FakeData.account.copy(deletedAt = now())).status)

        val client = createClient(token = null)
        assertEquals(expected = HttpStatusCode.Unauthorized, actual = client.post(request = HttpRequest(url = URL.Login), body = FakeData.credentials).status)
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
        val expiredAuth = seedUser(registrationForm = registrationForm(username = "expired_session_user", permissions = emptyMap()))

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

    /** Verifies that registering requires authentication. */
    @Test
    fun registerUnauthenticated() = runServerTest {
        installRouting()
        val client = createClient(token = null)

        assertEquals(expected = HttpStatusCode.Unauthorized, actual = client.post(request = HttpRequest(url = URL.Register), body = FakeData.registrationForm).status)
    }

    /** Verifies that logging in with a wrong password or an unknown username is rejected. */
    @Test
    fun loginWithInvalidCredentials() = runServerTest {
        installRouting()
        val client = createClient(token = null)

        assertEquals(
            expected = HttpStatusCode.Unauthorized,
            actual = client.post(request = HttpRequest(url = URL.Login), body = UserCredentials(username = FakeData.adminCredentials.username, password = "wrong_password")).status
        )
        assertEquals(
            expected = HttpStatusCode.Unauthorized,
            actual = client.post(request = HttpRequest(url = URL.Login), body = UserCredentials(username = "unknown", password = "pass123")).status
        )
    }
}
