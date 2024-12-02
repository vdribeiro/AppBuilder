package com.app.builder.domain.route.provider

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.seconds
import io.ktor.client.call.body
import io.ktor.client.request.get as ktorGet
import io.ktor.http.HttpStatusCode
import com.app.builder.core.locale.now
import com.app.builder.core.security.uuid
import com.app.builder.data.http.HttpRequest
import com.app.builder.data.http.Query
import com.app.builder.data.http.URL
import com.app.builder.data.http.get
import com.app.builder.data.http.post
import com.app.builder.domain.EntityType
import com.app.builder.domain.Permission
import com.app.builder.domain.RegistrationForm
import com.app.builder.domain.Task
import com.app.builder.domain.UserCredentials
import com.app.builder.test.FakeData
import com.app.builder.test.TestCase

class TaskTest: TestCase() {

    /** Verifies that fetching tasks requires authentication and reflects previously posted tasks. */
    @Test
    fun getTasks() = runServerTest {
        installRouting()

        val client = createClient(token = null)
        assertEquals(expected = HttpStatusCode.Unauthorized, actual = client.get(request = HttpRequest(url = URL.Tasks)).status)

        val authenticatedClient = createClient(token = adminBearerToken.accessToken)
        val tasksRequest = HttpRequest(url = URL.Tasks, queryMap = mapOf(Query.LastSyncUtc to "1970-01-01T00:00:00Z"))

        val emptyResponse = authenticatedClient.get(request = tasksRequest)
        assertEquals(expected = HttpStatusCode.OK, actual = emptyResponse.status)
        assertTrue(actual = emptyResponse.body<List<Task>>().isEmpty())

        authenticatedClient.post(request = HttpRequest(url = URL.Tasks), body = FakeData.task)
        val listResponse = authenticatedClient.get(request = tasksRequest)
        assertEquals(expected = HttpStatusCode.OK, actual = listResponse.status)
        assertEquals(expected = 1, actual = listResponse.body<List<Task>>().size)
    }

    /** Verifies that upserting a task requires authentication and a valid body, and returns the saved task. */
    @Test
    fun upsertTask() = runServerTest {
        installRouting()

        val client = createClient(token = null)
        assertEquals(expected = HttpStatusCode.Unauthorized, actual = client.post(request = HttpRequest(url = URL.Tasks), body = FakeData.task).status)

        val authenticatedClient = createClient(token = adminBearerToken.accessToken)
        val authenticatedRequest = HttpRequest(url = URL.Tasks)

        assertEquals(expected = HttpStatusCode.BadRequest, actual = authenticatedClient.post(request = authenticatedRequest, body = "invalid body").status)

        val response = authenticatedClient.post(request = authenticatedRequest, body = FakeData.task)
        assertEquals(expected = HttpStatusCode.OK, actual = response.status)
        assertEquals(expected = FakeData.task, actual = response.body<Task>())
    }

    /** Verifies fetching a single task by uuid: returns the task, 404s when missing, and 400s on a malformed uuid. */
    @Test
    fun getTask() = runServerTest {
        installRouting()
        val client = createClient(token = adminBearerToken.accessToken)

        assertEquals(expected = HttpStatusCode.BadRequest, actual = client.ktorGet(urlString = "${URL.Tasks.path}/not-a-uuid").status)
        assertEquals(expected = HttpStatusCode.NotFound, actual = client.ktorGet(urlString = "${URL.Tasks.path}/${uuid()}").status)

        client.post(request = HttpRequest(url = URL.Tasks), body = FakeData.task)
        val response = client.ktorGet(urlString = "${URL.Tasks.path}/${FakeData.task.uuid}")
        assertEquals(expected = HttpStatusCode.OK, actual = response.status)
        assertEquals(expected = FakeData.task, actual = response.body<Task>())
    }

    /** Verifies that fetching tasks paginates using the modifiedAt/uuid cursor. */
    @Test
    fun getTasksPaginated() = runServerTest {
        installRouting()
        val client = createClient(token = adminBearerToken.accessToken)

        val first = FakeData.task.copy(uuid = uuid(), modifiedAt = now())
        val second = FakeData.task.copy(uuid = uuid(), modifiedAt = now() + 1.seconds)
        client.post(request = HttpRequest(url = URL.Tasks), body = first)
        client.post(request = HttpRequest(url = URL.Tasks), body = second)

        val firstPageRequest = HttpRequest(url = URL.Tasks, queryMap = mapOf(Query.LastSyncUtc to "1970-01-01T00:00:00Z", Query.PageSize to "1"))
        val firstPageResponse = client.get(request = firstPageRequest).body<List<Task>>()
        assertEquals(expected = listOf(second), actual = firstPageResponse)

        val secondPageRequest = HttpRequest(
            url = URL.Tasks,
            queryMap = mapOf(
                Query.LastSyncUtc to "1970-01-01T00:00:00Z",
                Query.PageSize to "1",
                Query.CursorUtc to firstPageResponse.last().modifiedAt.toString(),
                Query.CursorUuid to firstPageResponse.last().uuid.toString()
            )
        )
        assertEquals(expected = listOf(first), actual = client.get(request = secondPageRequest).body<List<Task>>())
    }

    /** Verifies that a non-owning user cannot update another user's existing task. */
    @Test
    fun upsertTaskForbiddenForNonOwner() = runServerTest {
        installRouting()

        val ownerClient = createClient(token = adminBearerToken.accessToken)
        ownerClient.post(request = HttpRequest(url = URL.Tasks), body = FakeData.task)

        val otherUser = FakeData.user.copy(uuid = uuid(), permissions = mapOf(EntityType.TASK to Permission.WRITE))
        val otherAuth = seedUser(
            registrationForm = RegistrationForm(
                user = otherUser,
                credentials = UserCredentials(username = "other_task_user", password = "pass123")
            )
        )
        val otherClient = createClient(token = otherAuth.bearer.accessToken)

        val response = otherClient.post(request = HttpRequest(url = URL.Tasks), body = FakeData.task.copy(title = "Hijacked"))
        assertEquals(expected = HttpStatusCode.Forbidden, actual = response.status)
    }
}
