package com.app.builder.domain.route.provider

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import io.ktor.client.call.body
import io.ktor.http.HttpStatusCode
import com.app.builder.core.security.uuid
import com.app.builder.data.http.HttpRequest
import com.app.builder.data.http.Query
import com.app.builder.data.http.URL
import com.app.builder.data.http.get
import com.app.builder.data.http.post
import com.app.builder.domain.EntityType
import com.app.builder.domain.Permission
import com.app.builder.domain.Task
import com.app.builder.domain.TaskUserAccess
import com.app.builder.test.FakeData
import com.app.builder.test.TestCase
import io.ktor.client.request.get as ktorGet

class TaskAccessTest: TestCase() {

    /** Verifies that task access routes require authentication and the task permission. */
    @Test
    fun taskAccessRoutesRequireAuthenticationAndPermission() = runServerTest {
        installRouting()

        val client = createClient(token = null)
        assertEquals(expected = HttpStatusCode.Unauthorized, actual = client.get(request = HttpRequest(url = URL.TaskAccesses)).status)
        assertEquals(expected = HttpStatusCode.Unauthorized, actual = client.post(request = HttpRequest(url = URL.TaskAccesses), body = FakeData.taskAccess).status)

        // FakeData.account only has TASK -> READ.
        val userClient = createClient(token = bearerToken.accessToken)
        assertEquals(expected = HttpStatusCode.OK, actual = userClient.get(request = HttpRequest(url = URL.TaskAccesses)).status)
        assertEquals(expected = HttpStatusCode.Forbidden, actual = userClient.post(request = HttpRequest(url = URL.TaskAccesses), body = FakeData.taskAccess).status)

        val noTaskAuth = seedUser(registrationForm = registrationForm(username = "no_task_user", permissions = mapOf(EntityType.TASK_ACCESS to Permission.WRITE)))
        val noTaskClient = createClient(token = noTaskAuth.bearer.accessToken)
        assertEquals(expected = HttpStatusCode.Forbidden, actual = noTaskClient.get(request = HttpRequest(url = URL.TaskAccesses)).status)
        assertEquals(expected = HttpStatusCode.Forbidden, actual = noTaskClient.post(request = HttpRequest(url = URL.TaskAccesses), body = FakeData.taskAccess).status)
    }

    /** Verifies that granting an access returns the stored access, keeping the uuid of an existing one, and syncs it and the task to the user. */
    @Test
    fun grantAccess() = runServerTest {
        installRouting()
        val adminClient = createClient(token = adminBearerToken.accessToken)
        val userClient = createClient(token = bearerToken.accessToken)

        assertEquals(expected = HttpStatusCode.BadRequest, actual = adminClient.post(request = HttpRequest(url = URL.TaskAccesses), body = "invalid body").status)
        adminClient.post(request = HttpRequest(url = URL.Tasks), body = FakeData.task)

        val response = adminClient.post(request = HttpRequest(url = URL.TaskAccesses), body = FakeData.taskAccess)
        assertEquals(expected = HttpStatusCode.OK, actual = response.status)
        assertEquals(expected = FakeData.taskAccess, actual = response.body<TaskUserAccess>())

        val regranted = FakeData.taskAccess.copy(uuid = uuid(), modifiedAt = FakeData.taskAccess.modifiedAt + 1.seconds)
        val regrantedResponse = adminClient.post(request = HttpRequest(url = URL.TaskAccesses), body = regranted)
        assertEquals(expected = HttpStatusCode.OK, actual = regrantedResponse.status)
        assertEquals(expected = regranted.copy(uuid = FakeData.taskAccess.uuid), actual = regrantedResponse.body<TaskUserAccess>())

        val accessesRequest = HttpRequest(url = URL.TaskAccesses, queryMap = mapOf(Query.LastSyncUtc to "1970-01-01T00:00:00Z"))
        assertEquals(expected = 2, actual = userClient.get(request = accessesRequest).body<List<TaskUserAccess>>().size)
        assertEquals(expected = listOf(FakeData.task), actual = userClient.get(request = HttpRequest(url = URL.Tasks)).body<List<Task>>())
    }

    /** Verifies that an older write is answered with the newer stored access. */
    @Test
    fun upsertAccessLastWriteWins() = runServerTest {
        installRouting()
        val adminClient = createClient(token = adminBearerToken.accessToken)

        adminClient.post(request = HttpRequest(url = URL.Tasks), body = FakeData.task)
        adminClient.post(request = HttpRequest(url = URL.TaskAccesses), body = FakeData.taskAccess)

        val staleRevoke = FakeData.taskAccess.copy(modifiedAt = FakeData.taskAccess.modifiedAt - 1.minutes, deletedAt = FakeData.taskAccess.modifiedAt - 1.minutes)
        val response = adminClient.post(request = HttpRequest(url = URL.TaskAccesses), body = staleRevoke)
        assertEquals(expected = HttpStatusCode.OK, actual = response.status)
        assertEquals(expected = FakeData.taskAccess, actual = response.body<TaskUserAccess>())
    }

    /** Verifies that revoking an access hides the task from the user, who still syncs its own revoked access. */
    @Test
    fun revokeAccess() = runServerTest {
        installRouting()
        val adminClient = createClient(token = adminBearerToken.accessToken)
        val userClient = createClient(token = bearerToken.accessToken)

        adminClient.post(request = HttpRequest(url = URL.Tasks), body = FakeData.task)
        adminClient.post(request = HttpRequest(url = URL.TaskAccesses), body = FakeData.taskAccess)
        assertEquals(expected = HttpStatusCode.OK, actual = userClient.ktorGet(urlString = "${URL.Tasks.path}/${FakeData.task.uuid}").status)

        val revoked = FakeData.taskAccess.copy(modifiedAt = FakeData.taskAccess.modifiedAt + 1.seconds, deletedAt = FakeData.taskAccess.modifiedAt + 1.seconds)
        val response = adminClient.post(request = HttpRequest(url = URL.TaskAccesses), body = revoked)
        assertEquals(expected = HttpStatusCode.OK, actual = response.status)
        assertEquals(expected = revoked, actual = response.body<TaskUserAccess>())

        assertEquals(expected = HttpStatusCode.NotFound, actual = userClient.ktorGet(urlString = "${URL.Tasks.path}/${FakeData.task.uuid}").status)
        assertTrue(actual = userClient.get(request = HttpRequest(url = URL.Tasks)).body<List<Task>>().isEmpty())
        assertEquals(expected = listOf(revoked), actual = userClient.get(request = HttpRequest(url = URL.TaskAccesses)).body<List<TaskUserAccess>>())
    }

    /** Verifies that a caller with task write permission but no access to the task, or a missing task or user, gets a 404, and a reused uuid a 409. */
    @Test
    fun upsertAccessRefused() = runServerTest {
        installRouting()
        val adminClient = createClient(token = adminBearerToken.accessToken)
        val otherTask = FakeData.task.copy(uuid = uuid())

        assertEquals(expected = HttpStatusCode.NotFound, actual = adminClient.post(request = HttpRequest(url = URL.TaskAccesses), body = FakeData.taskAccess).status)
        adminClient.post(request = HttpRequest(url = URL.Tasks), body = FakeData.task)
        adminClient.post(request = HttpRequest(url = URL.Tasks), body = otherTask)
        assertEquals(expected = HttpStatusCode.NotFound, actual = adminClient.post(request = HttpRequest(url = URL.TaskAccesses), body = FakeData.taskAccess.copy(userUuid = uuid())).status)

        val writerAuth = seedUser(registrationForm = registrationForm(username = "task_writer", permissions = mapOf(EntityType.TASK to Permission.WRITE)))
        val writerClient = createClient(token = writerAuth.bearer.accessToken)
        assertEquals(expected = HttpStatusCode.NotFound, actual = writerClient.post(request = HttpRequest(url = URL.TaskAccesses), body = FakeData.taskAccess).status)

        assertEquals(expected = HttpStatusCode.OK, actual = adminClient.post(request = HttpRequest(url = URL.TaskAccesses), body = FakeData.taskAccess).status)
        assertEquals(expected = HttpStatusCode.Conflict, actual = adminClient.post(request = HttpRequest(url = URL.TaskAccesses), body = FakeData.taskAccess.copy(taskUuid = otherTask.uuid)).status)
    }
}
