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
import com.app.builder.domain.EntityType
import com.app.builder.domain.Permission
import com.app.builder.domain.Equipment
import com.app.builder.test.FakeData
import com.app.builder.test.TestCase
import io.ktor.client.request.get as ktorGet

class EquipmentTest: TestCase() {

    /** Verifies that fetching equipments requires authentication and reflects previously posted equipments. */
    @Test
    fun getEquipments() = runServerTest {
        installRouting()

        val client = createClient(token = null)
        assertEquals(expected = HttpStatusCode.Unauthorized, actual = client.get(request = HttpRequest(url = URL.Equipments)).status)

        val authenticatedClient = createClient(token = adminBearerToken.accessToken)
        val equipmentsRequest = HttpRequest(url = URL.Equipments, queryMap = mapOf(Query.LastSyncUtc to "1970-01-01T00:00:00Z"))

        val emptyResponse = authenticatedClient.get(request = equipmentsRequest)
        assertEquals(expected = HttpStatusCode.OK, actual = emptyResponse.status)
        assertTrue(actual = emptyResponse.body<List<Equipment>>().isEmpty())

        authenticatedClient.post(request = HttpRequest(url = URL.Equipments), body = FakeData.equipment)
        val listResponse = authenticatedClient.get(request = equipmentsRequest)
        assertEquals(expected = HttpStatusCode.OK, actual = listResponse.status)
        assertEquals(expected = 1, actual = listResponse.body<List<Equipment>>().size)
    }

    /** Verifies that upserting an equipment requires authentication and a valid body, and returns the saved equipment. */
    @Test
    fun upsertEquipment() = runServerTest {
        installRouting()

        val client = createClient(token = null)
        assertEquals(expected = HttpStatusCode.Unauthorized, actual = client.post(request = HttpRequest(url = URL.Equipments), body = FakeData.equipment).status)

        val authenticatedClient = createClient(token = adminBearerToken.accessToken)
        val authenticatedRequest = HttpRequest(url = URL.Equipments)

        assertEquals(expected = HttpStatusCode.BadRequest, actual = authenticatedClient.post(request = authenticatedRequest, body = "invalid body").status)

        val response = authenticatedClient.post(request = authenticatedRequest, body = FakeData.equipment)
        assertEquals(expected = HttpStatusCode.OK, actual = response.status)
        assertEquals(expected = FakeData.equipment, actual = response.body<Equipment>())
    }

    /** Verifies fetching a single equipment by uuid: returns the equipment, 404s when missing, and 400s on a malformed uuid. */
    @Test
    fun getEquipment() = runServerTest {
        installRouting()
        val client = createClient(token = adminBearerToken.accessToken)

        assertEquals(expected = HttpStatusCode.BadRequest, actual = client.ktorGet(urlString = "${URL.Equipments.path}/not-a-uuid").status)
        assertEquals(expected = HttpStatusCode.NotFound, actual = client.ktorGet(urlString = "${URL.Equipments.path}/${uuid()}").status)

        client.post(request = HttpRequest(url = URL.Equipments), body = FakeData.equipment)
        val response = client.ktorGet(urlString = "${URL.Equipments.path}/${FakeData.equipment.uuid}")
        assertEquals(expected = HttpStatusCode.OK, actual = response.status)
        assertEquals(expected = FakeData.equipment, actual = response.body<Equipment>())
    }

    /** Verifies that fetching equipments paginates using the modifiedAt/uuid cursor. */
    @Test
    fun getEquipmentsPaginated() = runServerTest {
        installRouting()
        val client = createClient(token = adminBearerToken.accessToken)

        val first = FakeData.equipment.copy(uuid = uuid(), modifiedAt = now())
        val second = FakeData.equipment.copy(uuid = uuid(), modifiedAt = now() + 1.seconds)
        client.post(request = HttpRequest(url = URL.Equipments), body = first)
        client.post(request = HttpRequest(url = URL.Equipments), body = second)

        val firstPageRequest = HttpRequest(url = URL.Equipments, queryMap = mapOf(Query.LastSyncUtc to "1970-01-01T00:00:00Z", Query.PageSize to "1"))
        val firstPageResponse = client.get(request = firstPageRequest).body<List<Equipment>>()
        assertEquals(expected = listOf(second), actual = firstPageResponse)

        val secondPageRequest = HttpRequest(
            url = URL.Equipments,
            queryMap = mapOf(
                Query.LastSyncUtc to "1970-01-01T00:00:00Z",
                Query.PageSize to "1",
                Query.CursorUtc to firstPageResponse.last().modifiedAt.toString(),
                Query.CursorUuid to firstPageResponse.last().uuid.toString()
            )
        )
        assertEquals(expected = listOf(first), actual = client.get(request = secondPageRequest).body<List<Equipment>>())
    }

    /** Verifies that any user with equipment write permission can update an equipment created by another user. */
    @Test
    fun upsertEquipmentCreatedByAnotherUser() = runServerTest {
        installRouting()

        val creatorClient = createClient(token = adminBearerToken.accessToken)
        creatorClient.post(request = HttpRequest(url = URL.Equipments), body = FakeData.equipment)

        val otherAuth = seedUser(registrationForm = registrationForm(username = "other_equipment_user", permissions = mapOf(EntityType.EQUIPMENT to Permission.WRITE)))
        val otherClient = createClient(token = otherAuth.bearer.accessToken)

        val editedEquipment = FakeData.equipment.copy(name = "Edited")
        assertEquals(expected = HttpStatusCode.OK, actual = otherClient.post(request = HttpRequest(url = URL.Equipments), body = editedEquipment).status)
        assertEquals(expected = editedEquipment, actual = otherClient.ktorGet(urlString = "${URL.Equipments.path}/${FakeData.equipment.uuid}").body<Equipment>())
    }

    /** Verifies that fetching a single equipment requires authentication, and that upserting requires equipment write permission. */
    @Test
    fun equipmentRoutesRequireAuthenticationAndPermission() = runServerTest {
        installRouting()

        val client = createClient(token = null)
        assertEquals(expected = HttpStatusCode.Unauthorized, actual = client.ktorGet(urlString = "${URL.Equipments.path}/${FakeData.equipment.uuid}").status)

        // FakeData.account only has EQUIPMENT -> READ.
        val userClient = createClient(token = bearerToken.accessToken)
        assertEquals(expected = HttpStatusCode.OK, actual = userClient.get(request = HttpRequest(url = URL.Equipments)).status)
        assertEquals(expected = HttpStatusCode.Forbidden, actual = userClient.post(request = HttpRequest(url = URL.Equipments), body = FakeData.equipment).status)
    }

    /** Verifies that reading equipments is forbidden for a user without any equipment permission. */
    @Test
    fun getEquipmentsRequiresReadPermission() = runServerTest {
        installRouting()

        val noEquipmentAuth = seedUser(registrationForm = registrationForm(username = "no_equipment_user", permissions = mapOf(EntityType.USER to Permission.READ)))
        val client = createClient(token = noEquipmentAuth.bearer.accessToken)

        assertEquals(expected = HttpStatusCode.Forbidden, actual = client.get(request = HttpRequest(url = URL.Equipments)).status)
        assertEquals(expected = HttpStatusCode.Forbidden, actual = client.ktorGet(urlString = "${URL.Equipments.path}/${FakeData.equipment.uuid}").status)
    }

    /** Verifies that a database failure while listing equipments answers 500 rather than an empty list, so a client never takes it as having nothing new to sync. */
    @Test
    fun getEquipmentsDatabaseFailure() = runServerTest {
        installRouting()
        val authenticatedClient = createClient(token = adminBearerToken.accessToken)
        dependency.get().database.reset()

        assertEquals(expected = HttpStatusCode.InternalServerError, actual = authenticatedClient.get(request = HttpRequest(url = URL.Equipments)).status)
    }
}
