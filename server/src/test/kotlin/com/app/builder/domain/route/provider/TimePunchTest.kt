package com.app.builder.domain.route.provider

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.seconds
import io.ktor.client.call.body
import io.ktor.http.HttpStatusCode
import com.app.builder.core.config.ServerFlags
import com.app.builder.core.locale.now
import com.app.builder.core.security.uuid
import com.app.builder.data.database.reset
import com.app.builder.data.http.HttpRequest
import com.app.builder.data.http.Query
import com.app.builder.data.http.URL
import com.app.builder.data.http.get
import com.app.builder.data.http.post
import com.app.builder.domain.EntityType
import com.app.builder.domain.Permission
import com.app.builder.domain.TimePunch
import com.app.builder.test.FakeData
import com.app.builder.test.TestCase
import io.ktor.client.request.get as ktorGet

class TimePunchTest: TestCase() {

    /** A time punch of the admin user. */
    private val adminTimePunch = TimePunch(userUuid = FakeData.adminUser.uuid)

    override suspend fun beforeTest() {
        super.beforeTest()
        ServerFlags.set { it.copy(timePunches = true) }
    }

    /** Verifies that fetching time punches requires authentication and reflects previously posted time punches. */
    @Test
    fun getTimePunches() = runServerTest {
        installRouting()

        val client = createClient(token = null)
        assertEquals(expected = HttpStatusCode.Unauthorized, actual = client.get(request = HttpRequest(url = URL.TimePunches)).status)

        val authenticatedClient = createClient(token = adminBearerToken.accessToken)
        val timePunchesRequest = HttpRequest(url = URL.TimePunches, queryMap = mapOf(Query.LastSyncUtc to "1970-01-01T00:00:00Z"))

        val emptyResponse = authenticatedClient.get(request = timePunchesRequest)
        assertEquals(expected = HttpStatusCode.OK, actual = emptyResponse.status)
        assertTrue(actual = emptyResponse.body<List<TimePunch>>().isEmpty())

        authenticatedClient.post(request = HttpRequest(url = URL.TimePunches), body = adminTimePunch)
        val listResponse = authenticatedClient.get(request = timePunchesRequest)
        assertEquals(expected = HttpStatusCode.OK, actual = listResponse.status)
        assertEquals(expected = listOf(adminTimePunch), actual = listResponse.body<List<TimePunch>>())
    }

    /** Verifies that upserting a time punch requires authentication and a valid body, and returns the saved time punch. */
    @Test
    fun upsertTimePunch() = runServerTest {
        installRouting()

        val client = createClient(token = null)
        assertEquals(expected = HttpStatusCode.Unauthorized, actual = client.post(request = HttpRequest(url = URL.TimePunches), body = adminTimePunch).status)

        val authenticatedClient = createClient(token = adminBearerToken.accessToken)
        val authenticatedRequest = HttpRequest(url = URL.TimePunches)

        assertEquals(expected = HttpStatusCode.BadRequest, actual = authenticatedClient.post(request = authenticatedRequest, body = "invalid body").status)

        val response = authenticatedClient.post(request = authenticatedRequest, body = adminTimePunch)
        assertEquals(expected = HttpStatusCode.OK, actual = response.status)
        assertEquals(expected = adminTimePunch, actual = response.body<TimePunch>())
    }

    /** Verifies fetching a single time punch by uuid: returns the time punch, 404s when missing, and 400s on a malformed uuid. */
    @Test
    fun getTimePunch() = runServerTest {
        installRouting()
        val client = createClient(token = adminBearerToken.accessToken)

        assertEquals(expected = HttpStatusCode.BadRequest, actual = client.ktorGet(urlString = "${URL.TimePunches.path}/not-a-uuid").status)
        assertEquals(expected = HttpStatusCode.NotFound, actual = client.ktorGet(urlString = "${URL.TimePunches.path}/${uuid()}").status)

        client.post(request = HttpRequest(url = URL.TimePunches), body = adminTimePunch)
        val response = client.ktorGet(urlString = "${URL.TimePunches.path}/${adminTimePunch.uuid}")
        assertEquals(expected = HttpStatusCode.OK, actual = response.status)
        assertEquals(expected = adminTimePunch, actual = response.body<TimePunch>())
    }

    /** Verifies that fetching time punches paginates using the modifiedAt/uuid cursor. */
    @Test
    fun getTimePunchesPaginated() = runServerTest {
        installRouting()
        val client = createClient(token = adminBearerToken.accessToken)

        val first = adminTimePunch.copy(uuid = uuid(), modifiedAt = now())
        val second = adminTimePunch.copy(uuid = uuid(), modifiedAt = now() + 1.seconds)
        client.post(request = HttpRequest(url = URL.TimePunches), body = first)
        client.post(request = HttpRequest(url = URL.TimePunches), body = second)

        val firstPageRequest = HttpRequest(url = URL.TimePunches, queryMap = mapOf(Query.LastSyncUtc to "1970-01-01T00:00:00Z", Query.PageSize to "1"))
        val firstPageResponse = client.get(request = firstPageRequest).body<List<TimePunch>>()
        assertEquals(expected = listOf(second), actual = firstPageResponse)

        val secondPageRequest = HttpRequest(
            url = URL.TimePunches,
            queryMap = mapOf(
                Query.LastSyncUtc to "1970-01-01T00:00:00Z",
                Query.PageSize to "1",
                Query.CursorUtc to firstPageResponse.last().modifiedAt.toString(),
                Query.CursorUuid to firstPageResponse.last().uuid.toString()
            )
        )
        assertEquals(expected = listOf(first), actual = client.get(request = secondPageRequest).body<List<TimePunch>>())
    }

    /** Verifies that a user without any time punch permission creates its own time punch and reads it back, alone, from the list and by uuid. */
    @Test
    fun userWithoutPermissionReachesItsOwnTimePunches() = runServerTest {
        installRouting()

        val adminClient = createClient(token = adminBearerToken.accessToken)
        adminClient.post(request = HttpRequest(url = URL.TimePunches), body = adminTimePunch)

        // FakeData.account has no time punch permission.
        val userClient = createClient(token = bearerToken.accessToken)
        val response = userClient.post(request = HttpRequest(url = URL.TimePunches), body = FakeData.timePunch)
        assertEquals(expected = HttpStatusCode.OK, actual = response.status)
        assertEquals(expected = FakeData.timePunch, actual = response.body<TimePunch>())

        assertEquals(expected = listOf(FakeData.timePunch), actual = userClient.get(request = HttpRequest(url = URL.TimePunches)).body<List<TimePunch>>())
        assertEquals(expected = HttpStatusCode.OK, actual = userClient.ktorGet(urlString = "${URL.TimePunches.path}/${FakeData.timePunch.uuid}").status)
        assertEquals(expected = HttpStatusCode.Forbidden, actual = userClient.ktorGet(urlString = "${URL.TimePunches.path}/${adminTimePunch.uuid}").status)

        assertEquals(expected = 2, actual = adminClient.get(request = HttpRequest(url = URL.TimePunches)).body<List<TimePunch>>().size)
    }

    /** Verifies that a user without time punch write permission can send its own time punch again unchanged, but can't change it, delete it or create one for another user. */
    @Test
    fun userWithoutPermissionOnlyCreatesItsOwnTimePunch() = runServerTest {
        installRouting()

        val userClient = createClient(token = bearerToken.accessToken)
        val request = HttpRequest(url = URL.TimePunches)
        assertEquals(expected = HttpStatusCode.OK, actual = userClient.post(request = request, body = FakeData.timePunch).status)
        assertEquals(expected = HttpStatusCode.OK, actual = userClient.post(request = request, body = FakeData.timePunch).status)

        assertEquals(expected = HttpStatusCode.Forbidden, actual = userClient.post(request = request, body = FakeData.timePunch.copy(time = now() - 1.hours)).status)
        assertEquals(expected = HttpStatusCode.Forbidden, actual = userClient.post(request = request, body = FakeData.timePunch.copy(deletedAt = now())).status)
        assertEquals(expected = HttpStatusCode.Forbidden, actual = userClient.post(request = request, body = TimePunch(userUuid = uuid(), deletedAt = now())).status)
        assertEquals(expected = HttpStatusCode.Forbidden, actual = userClient.post(request = request, body = adminTimePunch).status)

        val adminClient = createClient(token = adminBearerToken.accessToken)
        assertEquals(expected = FakeData.timePunch, actual = adminClient.ktorGet(urlString = "${URL.TimePunches.path}/${FakeData.timePunch.uuid}").body<TimePunch>())
    }

    /** Verifies that a user with time punch read permission reads the time punches of every user, but still only creates its own. */
    @Test
    fun userWithReadPermissionReadsEveryTimePunch() = runServerTest {
        installRouting()

        val adminClient = createClient(token = adminBearerToken.accessToken)
        adminClient.post(request = HttpRequest(url = URL.TimePunches), body = adminTimePunch)

        val readerAuth = seedUser(registrationForm = registrationForm(username = "time_punch_reader", permissions = mapOf(EntityType.TIME_PUNCH to Permission.READ)))
        val readerClient = createClient(token = readerAuth.bearer.accessToken)

        assertEquals(expected = listOf(adminTimePunch), actual = readerClient.get(request = HttpRequest(url = URL.TimePunches)).body<List<TimePunch>>())
        assertEquals(expected = adminTimePunch, actual = readerClient.ktorGet(urlString = "${URL.TimePunches.path}/${adminTimePunch.uuid}").body<TimePunch>())
        assertEquals(expected = HttpStatusCode.Forbidden, actual = readerClient.post(request = HttpRequest(url = URL.TimePunches), body = adminTimePunch.copy(deletedAt = now())).status)
        assertEquals(expected = HttpStatusCode.OK, actual = readerClient.post(request = HttpRequest(url = URL.TimePunches), body = TimePunch(userUuid = readerAuth.user.uuid)).status)
    }

    /** Verifies that a user with time punch write permission changes, deletes and creates the time punches of another user. */
    @Test
    fun userWithWritePermissionWritesEveryTimePunch() = runServerTest {
        installRouting()

        val userClient = createClient(token = bearerToken.accessToken)
        userClient.post(request = HttpRequest(url = URL.TimePunches), body = FakeData.timePunch)

        val adminClient = createClient(token = adminBearerToken.accessToken)
        val request = HttpRequest(url = URL.TimePunches)
        val editedTimePunch = FakeData.timePunch.copy(time = now() - 1.hours, modifiedAt = now() + 1.seconds)
        assertEquals(expected = HttpStatusCode.OK, actual = adminClient.post(request = request, body = editedTimePunch).status)
        assertEquals(expected = editedTimePunch, actual = adminClient.ktorGet(urlString = "${URL.TimePunches.path}/${FakeData.timePunch.uuid}").body<TimePunch>())

        assertEquals(expected = HttpStatusCode.OK, actual = adminClient.post(request = request, body = editedTimePunch.copy(deletedAt = now())).status)
        assertEquals(expected = HttpStatusCode.OK, actual = adminClient.post(request = request, body = TimePunch(userUuid = FakeData.user.uuid)).status)
    }

    /** Verifies that a database failure while listing time punches answers 500 rather than an empty list, so a client never takes it as having nothing new to sync. */
    @Test
    fun getTimePunchesDatabaseFailure() = runServerTest {
        installRouting()
        val authenticatedClient = createClient(token = adminBearerToken.accessToken)
        dependency.get().database.reset()

        assertEquals(expected = HttpStatusCode.InternalServerError, actual = authenticatedClient.get(request = HttpRequest(url = URL.TimePunches)).status)
    }
}
