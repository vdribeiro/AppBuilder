package com.app.builder.domain.route.provider

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.seconds
import io.ktor.client.call.body
import io.ktor.http.HttpStatusCode
import com.app.builder.core.locale.now
import com.app.builder.core.security.uuid
import com.app.builder.data.database.reset
import com.app.builder.data.http.HttpRequest
import com.app.builder.data.http.Query
import com.app.builder.data.http.URL
import com.app.builder.data.http.get
import com.app.builder.data.http.post
import com.app.builder.domain.Building
import com.app.builder.domain.EntityType
import com.app.builder.domain.Permission
import com.app.builder.test.FakeData
import com.app.builder.test.TestCase
import io.ktor.client.request.get as ktorGet

class BuildingTest: TestCase() {

    /** Verifies that fetching buildings requires authentication and reflects previously posted buildings. */
    @Test
    fun getBuildings() = runServerTest {
        installRouting()

        val client = createClient(token = null)
        assertEquals(expected = HttpStatusCode.Unauthorized, actual = client.get(request = HttpRequest(url = URL.Buildings)).status)

        val authenticatedClient = createClient(token = adminBearerToken.accessToken)
        val buildingsRequest = HttpRequest(url = URL.Buildings, queryMap = mapOf(Query.LastSyncUtc to "1970-01-01T00:00:00Z"))

        val emptyResponse = authenticatedClient.get(request = buildingsRequest)
        assertEquals(expected = HttpStatusCode.OK, actual = emptyResponse.status)
        assertTrue(actual = emptyResponse.body<List<Building>>().isEmpty())

        authenticatedClient.post(request = HttpRequest(url = URL.Buildings), body = FakeData.building)
        val listResponse = authenticatedClient.get(request = buildingsRequest)
        assertEquals(expected = HttpStatusCode.OK, actual = listResponse.status)
        assertEquals(expected = 1, actual = listResponse.body<List<Building>>().size)
    }

    /** Verifies that upserting a building requires authentication and a valid body, and returns the saved building. */
    @Test
    fun upsertBuilding() = runServerTest {
        installRouting()

        val client = createClient(token = null)
        assertEquals(expected = HttpStatusCode.Unauthorized, actual = client.post(request = HttpRequest(url = URL.Buildings), body = FakeData.building).status)

        val authenticatedClient = createClient(token = adminBearerToken.accessToken)
        val authenticatedRequest = HttpRequest(url = URL.Buildings)

        assertEquals(expected = HttpStatusCode.BadRequest, actual = authenticatedClient.post(request = authenticatedRequest, body = "invalid body").status)

        val response = authenticatedClient.post(request = authenticatedRequest, body = FakeData.building)
        assertEquals(expected = HttpStatusCode.OK, actual = response.status)
        assertEquals(expected = FakeData.building, actual = response.body<Building>())
    }

    /** Verifies that a building is accepted without coordinates, and rejected with only one or with out-of-range coordinates. */
    @Test
    fun upsertBuildingValidatesCoordinates() = runServerTest {
        installRouting()
        val client = createClient(token = adminBearerToken.accessToken)
        val request = HttpRequest(url = URL.Buildings)

        assertEquals(expected = HttpStatusCode.OK, actual = client.post(request = request, body = FakeData.building.copy(latitude = null, longitude = null)).status)
        listOf(
            FakeData.building.copy(longitude = null),
            FakeData.building.copy(latitude = null),
            FakeData.building.copy(latitude = 90.5),
            FakeData.building.copy(longitude = -180.5)
        ).forEach {
            assertEquals(expected = HttpStatusCode.BadRequest, actual = client.post(request = request, body = it).status, message = "$it")
        }
    }

    /** Verifies fetching a single building by uuid: returns the building, 404s when missing, and 400s on a malformed uuid. */
    @Test
    fun getBuilding() = runServerTest {
        installRouting()
        val client = createClient(token = adminBearerToken.accessToken)

        assertEquals(expected = HttpStatusCode.BadRequest, actual = client.ktorGet(urlString = "${URL.Buildings.path}/not-a-uuid").status)
        assertEquals(expected = HttpStatusCode.NotFound, actual = client.ktorGet(urlString = "${URL.Buildings.path}/${uuid()}").status)

        client.post(request = HttpRequest(url = URL.Buildings), body = FakeData.building)
        val response = client.ktorGet(urlString = "${URL.Buildings.path}/${FakeData.building.uuid}")
        assertEquals(expected = HttpStatusCode.OK, actual = response.status)
        assertEquals(expected = FakeData.building, actual = response.body<Building>())
    }

    /** Verifies that fetching buildings paginates using the modifiedAt/uuid cursor. */
    @Test
    fun getBuildingsPaginated() = runServerTest {
        installRouting()
        val client = createClient(token = adminBearerToken.accessToken)

        val first = FakeData.building.copy(uuid = uuid(), modifiedAt = now())
        val second = FakeData.building.copy(uuid = uuid(), modifiedAt = now() + 1.seconds)
        client.post(request = HttpRequest(url = URL.Buildings), body = first)
        client.post(request = HttpRequest(url = URL.Buildings), body = second)

        val firstPageRequest = HttpRequest(url = URL.Buildings, queryMap = mapOf(Query.LastSyncUtc to "1970-01-01T00:00:00Z", Query.PageSize to "1"))
        val firstPageResponse = client.get(request = firstPageRequest).body<List<Building>>()
        assertEquals(expected = listOf(second), actual = firstPageResponse)

        val secondPageRequest = HttpRequest(
            url = URL.Buildings,
            queryMap = mapOf(
                Query.LastSyncUtc to "1970-01-01T00:00:00Z",
                Query.PageSize to "1",
                Query.CursorUtc to firstPageResponse.last().modifiedAt.toString(),
                Query.CursorUuid to firstPageResponse.last().uuid.toString()
            )
        )
        assertEquals(expected = listOf(first), actual = client.get(request = secondPageRequest).body<List<Building>>())
    }

    /** Verifies that any user with building write permission can update a building created by another user. */
    @Test
    fun upsertBuildingCreatedByAnotherUser() = runServerTest {
        installRouting()

        val creatorClient = createClient(token = adminBearerToken.accessToken)
        creatorClient.post(request = HttpRequest(url = URL.Buildings), body = FakeData.building)

        val otherAuth = seedUser(registrationForm = registrationForm(username = "other_building_user", permissions = mapOf(EntityType.BUILDING to Permission.WRITE)))
        val otherClient = createClient(token = otherAuth.bearer.accessToken)

        val editedBuilding = FakeData.building.copy(name = "Edited")
        assertEquals(expected = HttpStatusCode.OK, actual = otherClient.post(request = HttpRequest(url = URL.Buildings), body = editedBuilding).status)
        assertEquals(expected = editedBuilding, actual = otherClient.ktorGet(urlString = "${URL.Buildings.path}/${FakeData.building.uuid}").body<Building>())
    }

    /** Verifies that fetching a single building requires authentication, and that upserting requires building write permission. */
    @Test
    fun buildingRoutesRequireAuthenticationAndPermission() = runServerTest {
        installRouting()

        val client = createClient(token = null)
        assertEquals(expected = HttpStatusCode.Unauthorized, actual = client.ktorGet(urlString = "${URL.Buildings.path}/${FakeData.building.uuid}").status)

        // FakeData.account only has BUILDING -> READ.
        val userClient = createClient(token = bearerToken.accessToken)
        assertEquals(expected = HttpStatusCode.OK, actual = userClient.get(request = HttpRequest(url = URL.Buildings)).status)
        assertEquals(expected = HttpStatusCode.Forbidden, actual = userClient.post(request = HttpRequest(url = URL.Buildings), body = FakeData.building).status)
    }

    /** Verifies that reading buildings is forbidden for a user without any building permission. */
    @Test
    fun getBuildingsRequiresReadPermission() = runServerTest {
        installRouting()

        val noBuildingAuth = seedUser(registrationForm = registrationForm(username = "no_building_user", permissions = mapOf(EntityType.USER to Permission.READ)))
        val client = createClient(token = noBuildingAuth.bearer.accessToken)

        assertEquals(expected = HttpStatusCode.Forbidden, actual = client.get(request = HttpRequest(url = URL.Buildings)).status)
        assertEquals(expected = HttpStatusCode.Forbidden, actual = client.ktorGet(urlString = "${URL.Buildings.path}/${FakeData.building.uuid}").status)
    }

    /** Verifies that a database failure while listing buildings answers 500 rather than an empty list, so a client never takes it as having nothing new to sync. */
    @Test
    fun getBuildingsDatabaseFailure() = runServerTest {
        installRouting()
        val authenticatedClient = createClient(token = adminBearerToken.accessToken)
        dependency.get().database.reset()

        assertEquals(expected = HttpStatusCode.InternalServerError, actual = authenticatedClient.get(request = HttpRequest(url = URL.Buildings)).status)
    }
}
