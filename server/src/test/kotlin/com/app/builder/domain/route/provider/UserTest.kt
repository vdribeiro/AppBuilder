package com.app.builder.domain.route.provider

import kotlin.test.Test
import kotlin.test.assertEquals
import io.ktor.client.call.body
import io.ktor.client.request.get as ktorGet
import io.ktor.http.HttpStatusCode
import com.app.builder.core.security.uuid
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

    /** Verifies that a user with session write permission can update another user's record, including their permissions. */
    @Test
    fun upsertUserPrivilegedCrossUserUpdate() = runServerTest {
        installRouting()
        val adminClient = createClient(token = adminBearerToken.accessToken)

        val updatedPermissions = FakeData.user.permissions + (EntityType.TASK to Permission.WRITE)
        val updatedUser = FakeData.user.copy(name = "Updated By Admin", permissions = updatedPermissions)

        val response = adminClient.post(request = HttpRequest(url = URL.Users), body = updatedUser)
        assertEquals(expected = HttpStatusCode.OK, actual = response.status)
        assertEquals(expected = updatedUser, actual = response.body<User>())

        val fetched = adminClient.ktorGet(urlString = "${URL.Users.path}/${updatedUser.uuid}")
        assertEquals(expected = HttpStatusCode.OK, actual = fetched.status)
        assertEquals(expected = updatedUser, actual = fetched.body<User>())
    }
}
