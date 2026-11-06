package com.app.builder.domain.route.provider

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import io.ktor.client.call.body
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import com.app.builder.core.locale.now
import com.app.builder.core.security.uuid
import com.app.builder.data.http.HttpRequest
import com.app.builder.data.http.URL
import com.app.builder.data.http.get
import com.app.builder.data.http.post
import com.app.builder.domain.Account
import com.app.builder.domain.DeviceToken
import com.app.builder.domain.EntityType
import com.app.builder.domain.Permission
import com.app.builder.test.FakeData
import com.app.builder.test.TestCase
import io.ktor.client.request.get as ktorGet

class AccountTest: TestCase() {

    /** Verifies fetching a single account by user uuid: requires authentication, returns the account without its password hash, 404s when missing, and 400s on a malformed uuid. */
    @Test
    fun getAccount() = runServerTest {
        installRouting()
        assertEquals(expected = HttpStatusCode.Unauthorized, actual = createClient(token = null).ktorGet(urlString = "${URL.Accounts.path}/${FakeData.adminUser.uuid}").status)

        // FakeData.account only has ACCOUNT -> READ.
        val client = createClient(token = bearerToken.accessToken)

        assertEquals(expected = HttpStatusCode.BadRequest, actual = client.ktorGet(urlString = "${URL.Accounts.path}/not-a-uuid").status)
        assertEquals(expected = HttpStatusCode.NotFound, actual = client.ktorGet(urlString = "${URL.Accounts.path}/${uuid()}").status)

        val response = client.ktorGet(urlString = "${URL.Accounts.path}/${FakeData.adminUser.uuid}")
        assertEquals(expected = HttpStatusCode.OK, actual = response.status)
        assertFalse(actual = response.bodyAsText().contains(other = "password", ignoreCase = true))
        assertEquals(expected = FakeData.adminAccount, actual = response.body<Account>())
    }

    /** Verifies that reading an account is forbidden for a user without any account permission. */
    @Test
    fun getAccountRequiresReadPermission() = runServerTest {
        installRouting()

        val noAccountAuth = seedUser(registrationForm = registrationForm(username = "no_account_permission", permissions = mapOf(EntityType.USER to Permission.WRITE)))
        val client = createClient(token = noAccountAuth.bearer.accessToken)

        assertEquals(expected = HttpStatusCode.Forbidden, actual = client.ktorGet(urlString = "${URL.Accounts.path}/${FakeData.user.uuid}").status)
    }

    /** Verifies that upserting an account requires authentication and the account write permission, 404s for a user without an account, 409s for a taken username, rejects invalid bodies, and stores the change. */
    @Test
    fun upsertAccount() = runServerTest {
        installRouting()
        val updated = FakeData.account.copy(username = "renamed", permissions = FakeData.account.permissions + (EntityType.TASK to Permission.WRITE))

        val client = createClient(token = null)
        assertEquals(expected = HttpStatusCode.Unauthorized, actual = client.post(request = HttpRequest(url = URL.Accounts), body = updated).status)

        // FakeData.account only has ACCOUNT -> READ, so it cannot change any account, not even its own.
        val userClient = createClient(token = bearerToken.accessToken)
        assertEquals(expected = HttpStatusCode.Forbidden, actual = userClient.post(request = HttpRequest(url = URL.Accounts), body = updated).status)

        val adminClient = createClient(token = adminBearerToken.accessToken)
        assertEquals(expected = HttpStatusCode.BadRequest, actual = adminClient.post(request = HttpRequest(url = URL.Accounts), body = "not an account").status)
        assertEquals(expected = HttpStatusCode.NotFound, actual = adminClient.post(request = HttpRequest(url = URL.Accounts), body = updated.copy(userUuid = uuid())).status)
        assertEquals(expected = HttpStatusCode.Conflict, actual = adminClient.post(request = HttpRequest(url = URL.Accounts), body = updated.copy(username = FakeData.adminAccount.username)).status)

        val response = adminClient.post(request = HttpRequest(url = URL.Accounts), body = updated)
        assertEquals(expected = HttpStatusCode.OK, actual = response.status)
        assertEquals(expected = updated, actual = response.body<Account>())
        assertEquals(expected = updated, actual = adminClient.ktorGet(urlString = "${URL.Accounts.path}/${FakeData.user.uuid}").body<Account>())
    }

    /** Verifies that a permission change applies to the account's existing session before its access token expires. */
    @Test
    fun upsertAccountAppliesPermissionsImmediately() = runServerTest {
        installRouting()

        val userClient = createClient(token = bearerToken.accessToken)
        assertEquals(expected = HttpStatusCode.OK, actual = userClient.get(request = HttpRequest(url = URL.Users)).status)

        val adminClient = createClient(token = adminBearerToken.accessToken)
        val revoked = FakeData.account.copy(permissions = FakeData.account.permissions - EntityType.USER)
        assertEquals(expected = HttpStatusCode.OK, actual = adminClient.post(request = HttpRequest(url = URL.Accounts), body = revoked).status)

        assertEquals(expected = HttpStatusCode.Forbidden, actual = userClient.get(request = HttpRequest(url = URL.Users)).status)
    }

    /** Verifies that disabling an account purges its user's device tokens and refuses its session further reads. */
    @Test
    fun disablingAccountPurgesDeviceTokens() = runServerTest {
        installRouting()
        val deviceTokenUseCases = dependency.get().useCases.deviceTokenUseCases

        val userClient = createClient(token = bearerToken.accessToken)
        assertEquals(expected = HttpStatusCode.OK, actual = userClient.post<DeviceToken>(request = HttpRequest(url = URL.DeviceTokens), body = DeviceToken(deviceUuid = uuid(), token = "fcmToken")).status)
        assertEquals(expected = 1, actual = deviceTokenUseCases.getTokens(userUuid = FakeData.user.uuid).size)

        val adminClient = createClient(token = adminBearerToken.accessToken)
        assertEquals(expected = HttpStatusCode.OK, actual = adminClient.post(request = HttpRequest(url = URL.Accounts), body = FakeData.account.copy(deletedAt = now())).status)

        assertTrue(actual = deviceTokenUseCases.getTokens(userUuid = FakeData.user.uuid).isEmpty())
        assertEquals(expected = HttpStatusCode.Forbidden, actual = userClient.get(request = HttpRequest(url = URL.Users)).status)
    }
}
