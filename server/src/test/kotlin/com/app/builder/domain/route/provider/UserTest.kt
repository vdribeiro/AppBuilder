package com.app.builder.domain.route.provider

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.time.Duration.Companion.milliseconds
import io.ktor.client.call.body
import io.ktor.http.HttpStatusCode
import com.app.builder.core.locale.now
import com.app.builder.core.security.uuid
import com.app.builder.data.http.Header
import com.app.builder.data.http.HttpRequest
import com.app.builder.data.http.Query
import com.app.builder.data.http.URL
import com.app.builder.data.http.get
import com.app.builder.data.http.post
import com.app.builder.domain.EntityType
import com.app.builder.domain.Permission
import com.app.builder.domain.User
import com.app.builder.test.FakeData
import com.app.builder.test.TestCase
import io.ktor.client.request.get as ktorGet

class UserTest: TestCase() {

    /** Verifies that fetching users requires authentication and supports pagination. */
    @Test
    fun getUsers() = runServerTest {
        installRouting()

        val client = createClient(token = null)
        assertEquals(expected = HttpStatusCode.Unauthorized, actual = client.get(request = HttpRequest(url = URL.Users)).status)

        val adminClient = createClient(token = bearerToken.accessToken)
        val request = HttpRequest(url = URL.Users, queryMap = mapOf(Query.LastSyncUtc to "1970-01-01T00:00:00Z"))

        val response = adminClient.get(request = request)
        assertEquals(expected = HttpStatusCode.OK, actual = response.status)
        val allUsers = response.body<List<User>>()
        assertEquals(expected = setOf(FakeData.adminUser, FakeData.user), actual = allUsers.toSet())

        val firstPageRequest = HttpRequest(url = URL.Users, queryMap = mapOf(Query.LastSyncUtc to "1970-01-01T00:00:00Z", Query.PageSize to "1"))
        val firstPageResponse = adminClient.get(request = firstPageRequest).body<List<User>>()
        assertEquals(expected = listOf(allUsers.first()), actual = firstPageResponse)

        val secondPageRequest = HttpRequest(
            url = URL.Users,
            queryMap = mapOf(
                Query.LastSyncUtc to "1970-01-01T00:00:00Z",
                Query.PageSize to "1",
                Query.CursorUtc to firstPageResponse.last().modifiedAt.toString(),
                Query.CursorUuid to firstPageResponse.last().uuid.toString()
            )
        )
        assertEquals(expected = listOf(allUsers.last()), actual = adminClient.get(request = secondPageRequest).body<List<User>>())
    }

    /** Verifies that upserting a user requires authentication, enforces ownership, and rejects invalid bodies. */
    @Test
    fun upsertUser() = runServerTest {
        installRouting()

        val client = createClient(token = null)
        assertEquals(expected = HttpStatusCode.Unauthorized, actual = client.post(request = HttpRequest(url = URL.Users), body = FakeData.adminUser).status)

        val authenticatedClient = createClient(token = bearerToken.accessToken)
        val response = authenticatedClient.post(request = HttpRequest(url = URL.Users), body = FakeData.user.copy(uuid = uuid()))
        assertEquals(expected = HttpStatusCode.Forbidden, actual = response.status)

        val newUser = FakeData.user.copy(name = "Renamed User")
        val userResponse = authenticatedClient.post(request = HttpRequest(url = URL.Users), body = newUser)
        assertEquals(expected = HttpStatusCode.OK, actual = userResponse.status)
        assertEquals(expected = newUser, actual = userResponse.body<User>())

        val adminClient = createClient(token = adminBearerToken.accessToken)
        assertEquals(expected = HttpStatusCode.BadRequest, actual = adminClient.post(request = HttpRequest(url = URL.Users), body = "not a user").status)

        val newAdmin = FakeData.adminUser.copy(name = "Renamed Admin")
        val adminResponse = adminClient.post(request = HttpRequest(url = URL.Users), body = newAdmin)
        assertEquals(expected = HttpStatusCode.OK, actual = adminResponse.status)
        assertEquals(expected = newAdmin, actual = adminResponse.body<User>())
    }

    /** Verifies fetching a single user by uuid: returns the user, 404s when missing, and 400s on a malformed uuid. */
    @Test
    fun getUser() = runServerTest {
        installRouting()
        val client = createClient(token = adminBearerToken.accessToken)

        assertEquals(expected = HttpStatusCode.BadRequest, actual = client.ktorGet(urlString = "${URL.Users.path}/not-a-uuid").status)
        assertEquals(expected = HttpStatusCode.NotFound, actual = client.ktorGet(urlString = "${URL.Users.path}/${uuid()}").status)

        val response = client.ktorGet(urlString = "${URL.Users.path}/${FakeData.adminUser.uuid}")
        assertEquals(expected = HttpStatusCode.OK, actual = response.status)
        assertEquals(expected = FakeData.adminUser, actual = response.body<User>())
    }

    /** Verifies that a user with user write permission can update another user's record. */
    @Test
    fun upsertUserPrivilegedCrossUserUpdate() = runServerTest {
        installRouting()
        val adminClient = createClient(token = adminBearerToken.accessToken)

        val updatedUser = FakeData.user.copy(name = "Updated By Admin", avatar = "avatar")

        val response = adminClient.post(request = HttpRequest(url = URL.Users), body = updatedUser)
        assertEquals(expected = HttpStatusCode.OK, actual = response.status)
        assertEquals(expected = updatedUser, actual = response.body<User>())

        val fetched = adminClient.ktorGet(urlString = "${URL.Users.path}/${updatedUser.uuid}")
        assertEquals(expected = HttpStatusCode.OK, actual = fetched.status)
        assertEquals(expected = updatedUser, actual = fetched.body<User>())
    }

    /** Verifies that a user whose account is disabled can still apply an edit to itself that predates the disabling, but never clear its own deletedAt, and that a later edit is refused. */
    @Test
    fun upsertUserCannotResurrectItself() = runServerTest {
        installRouting()

        val deletedAt = now()
        val deletedUser = FakeData.user.copy(deletedAt = deletedAt)
        val adminClient = createClient(token = adminBearerToken.accessToken)
        assertEquals(expected = HttpStatusCode.OK, actual = adminClient.post(request = HttpRequest(url = URL.Accounts), body = FakeData.account.copy(deletedAt = deletedAt)).status)
        assertEquals(expected = HttpStatusCode.OK, actual = adminClient.post(request = HttpRequest(url = URL.Users), body = deletedUser).status)

        val deletedUserClient = createClient(token = bearerToken.accessToken)
        val response = deletedUserClient.post(
            request = HttpRequest(url = URL.Users, headerMap = mapOf(Header.RequestUtc to (deletedAt - 60_000L.milliseconds).toString())),
            body = FakeData.user.copy(name = "Resurrected", deletedAt = null)
        )
        assertEquals(expected = HttpStatusCode.OK, actual = response.status)

        val later = deletedUserClient.post(
            request = HttpRequest(url = URL.Users, headerMap = mapOf(Header.RequestUtc to (deletedAt + 60_000L.milliseconds).toString())),
            body = FakeData.user.copy(name = "Later", deletedAt = null)
        )
        assertEquals(expected = HttpStatusCode.Forbidden, actual = later.status)

        val fetched = adminClient.ktorGet(urlString = "${URL.Users.path}/${FakeData.user.uuid}")
        assertEquals(expected = HttpStatusCode.OK, actual = fetched.status)
        assertEquals(expected = "Resurrected", actual = fetched.body<User>().name)
        assertNotNull(actual = fetched.body<User>().deletedAt)
    }

    /** Verifies that fetching a single user requires authentication, that reading users requires the user read permission, and that a user with only that permission can change itself but not others. */
    @Test
    fun userRoutesRequireAuthenticationAndPermission() = runServerTest {
        installRouting()

        val client = createClient(token = null)
        assertEquals(expected = HttpStatusCode.Unauthorized, actual = client.ktorGet(urlString = "${URL.Users.path}/${FakeData.user.uuid}").status)

        val noUserPermissionAuth = seedUser(registrationForm = registrationForm(username = "no_user_permission", permissions = mapOf(EntityType.TASK to Permission.READ)))
        val noUserPermissionClient = createClient(token = noUserPermissionAuth.bearer.accessToken)
        assertEquals(expected = HttpStatusCode.Forbidden, actual = noUserPermissionClient.get(request = HttpRequest(url = URL.Users)).status)
        assertEquals(expected = HttpStatusCode.Forbidden, actual = noUserPermissionClient.ktorGet(urlString = "${URL.Users.path}/${FakeData.user.uuid}").status)
        assertEquals(expected = HttpStatusCode.Forbidden, actual = noUserPermissionClient.post(request = HttpRequest(url = URL.Users), body = noUserPermissionAuth.user.copy(name = "Renamed")).status)

        val readOnlyAuth = seedUser(registrationForm = registrationForm(username = "read_only_user", permissions = mapOf(EntityType.USER to Permission.READ)))
        val readOnlyClient = createClient(token = readOnlyAuth.bearer.accessToken)
        assertEquals(expected = HttpStatusCode.OK, actual = readOnlyClient.get(request = HttpRequest(url = URL.Users)).status)
        assertEquals(expected = HttpStatusCode.OK, actual = readOnlyClient.post(request = HttpRequest(url = URL.Users), body = readOnlyAuth.user.copy(name = "Renamed")).status)
        assertEquals(expected = HttpStatusCode.Forbidden, actual = readOnlyClient.post(request = HttpRequest(url = URL.Users), body = FakeData.user.copy(name = "Renamed")).status)
    }

    /** Verifies that a user without user write permission cannot delete itself, as the stored deletedAt is carried over. */
    @Test
    fun upsertUserCannotDeleteItself() = runServerTest {
        installRouting()

        val client = createClient(token = bearerToken.accessToken)
        val response = client.post(request = HttpRequest(url = URL.Users), body = FakeData.user.copy(name = "Deleted", deletedAt = now()))
        assertEquals(expected = HttpStatusCode.OK, actual = response.status)
        assertEquals(expected = FakeData.user.copy(name = "Deleted"), actual = response.body<User>())

        val adminClient = createClient(token = adminBearerToken.accessToken)
        assertNull(actual = adminClient.ktorGet(urlString = "${URL.Users.path}/${FakeData.user.uuid}").body<User>().deletedAt)
    }

    /** Verifies that deleting a user is rejected while its account is active, and accepted once the account is disabled or when the user has no account. */
    @Test
    fun deleteUserRequiresDisabledAccount() = runServerTest {
        installRouting()
        val adminClient = createClient(token = adminBearerToken.accessToken)
        val deletedUser = FakeData.user.copy(deletedAt = now())

        assertEquals(expected = HttpStatusCode.Conflict, actual = adminClient.post(request = HttpRequest(url = URL.Users), body = deletedUser).status)
        assertNull(actual = adminClient.ktorGet(urlString = "${URL.Users.path}/${FakeData.user.uuid}").body<User>().deletedAt)

        assertEquals(expected = HttpStatusCode.OK, actual = adminClient.post(request = HttpRequest(url = URL.Accounts), body = FakeData.account.copy(deletedAt = now())).status)
        assertEquals(expected = HttpStatusCode.OK, actual = adminClient.post(request = HttpRequest(url = URL.Users), body = deletedUser).status)
        assertNotNull(actual = adminClient.ktorGet(urlString = "${URL.Users.path}/${FakeData.user.uuid}").body<User>().deletedAt)

        val userWithoutAccount = FakeData.user.copy(uuid = uuid(), name = "No Account")
        assertEquals(expected = HttpStatusCode.OK, actual = adminClient.post(request = HttpRequest(url = URL.Users), body = userWithoutAccount).status)
        assertEquals(expected = HttpStatusCode.OK, actual = adminClient.post(request = HttpRequest(url = URL.Users), body = userWithoutAccount.copy(deletedAt = now())).status)
    }
}
