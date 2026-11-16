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
import com.app.builder.domain.Material
import com.app.builder.domain.Permission
import com.app.builder.test.FakeData
import com.app.builder.test.TestCase
import io.ktor.client.request.get as ktorGet

class MaterialTest: TestCase() {

    /** Verifies that fetching materials requires authentication and reflects previously posted materials. */
    @Test
    fun getMaterials() = runServerTest {
        installRouting()

        val client = createClient(token = null)
        assertEquals(expected = HttpStatusCode.Unauthorized, actual = client.get(request = HttpRequest(url = URL.Materials)).status)

        val authenticatedClient = createClient(token = adminBearerToken.accessToken)
        val materialsRequest = HttpRequest(url = URL.Materials, queryMap = mapOf(Query.LastSyncUtc to "1970-01-01T00:00:00Z"))

        val emptyResponse = authenticatedClient.get(request = materialsRequest)
        assertEquals(expected = HttpStatusCode.OK, actual = emptyResponse.status)
        assertTrue(actual = emptyResponse.body<List<Material>>().isEmpty())

        authenticatedClient.post(request = HttpRequest(url = URL.Materials), body = FakeData.material)
        val listResponse = authenticatedClient.get(request = materialsRequest)
        assertEquals(expected = HttpStatusCode.OK, actual = listResponse.status)
        assertEquals(expected = 1, actual = listResponse.body<List<Material>>().size)
    }

    /** Verifies that upserting a material requires authentication and a valid body, and returns the saved material. */
    @Test
    fun upsertMaterial() = runServerTest {
        installRouting()

        val client = createClient(token = null)
        assertEquals(expected = HttpStatusCode.Unauthorized, actual = client.post(request = HttpRequest(url = URL.Materials), body = FakeData.material).status)

        val authenticatedClient = createClient(token = adminBearerToken.accessToken)
        val authenticatedRequest = HttpRequest(url = URL.Materials)

        assertEquals(expected = HttpStatusCode.BadRequest, actual = authenticatedClient.post(request = authenticatedRequest, body = "invalid body").status)

        val response = authenticatedClient.post(request = authenticatedRequest, body = FakeData.material)
        assertEquals(expected = HttpStatusCode.OK, actual = response.status)
        assertEquals(expected = FakeData.material, actual = response.body<Material>())
    }

    /** Verifies fetching a single material by uuid: returns the material, 404s when missing, and 400s on a malformed uuid. */
    @Test
    fun getMaterial() = runServerTest {
        installRouting()
        val client = createClient(token = adminBearerToken.accessToken)

        assertEquals(expected = HttpStatusCode.BadRequest, actual = client.ktorGet(urlString = "${URL.Materials.path}/not-a-uuid").status)
        assertEquals(expected = HttpStatusCode.NotFound, actual = client.ktorGet(urlString = "${URL.Materials.path}/${uuid()}").status)

        client.post(request = HttpRequest(url = URL.Materials), body = FakeData.material)
        val response = client.ktorGet(urlString = "${URL.Materials.path}/${FakeData.material.uuid}")
        assertEquals(expected = HttpStatusCode.OK, actual = response.status)
        assertEquals(expected = FakeData.material, actual = response.body<Material>())
    }

    /** Verifies that fetching materials paginates using the modifiedAt/uuid cursor. */
    @Test
    fun getMaterialsPaginated() = runServerTest {
        installRouting()
        val client = createClient(token = adminBearerToken.accessToken)

        val first = FakeData.material.copy(uuid = uuid(), modifiedAt = now())
        val second = FakeData.material.copy(uuid = uuid(), modifiedAt = now() + 1.seconds)
        client.post(request = HttpRequest(url = URL.Materials), body = first)
        client.post(request = HttpRequest(url = URL.Materials), body = second)

        val firstPageRequest = HttpRequest(url = URL.Materials, queryMap = mapOf(Query.LastSyncUtc to "1970-01-01T00:00:00Z", Query.PageSize to "1"))
        val firstPageResponse = client.get(request = firstPageRequest).body<List<Material>>()
        assertEquals(expected = listOf(second), actual = firstPageResponse)

        val secondPageRequest = HttpRequest(
            url = URL.Materials,
            queryMap = mapOf(
                Query.LastSyncUtc to "1970-01-01T00:00:00Z",
                Query.PageSize to "1",
                Query.CursorUtc to firstPageResponse.last().modifiedAt.toString(),
                Query.CursorUuid to firstPageResponse.last().uuid.toString()
            )
        )
        assertEquals(expected = listOf(first), actual = client.get(request = secondPageRequest).body<List<Material>>())
    }

    /** Verifies that any user with material write permission can update a material created by another user. */
    @Test
    fun upsertMaterialCreatedByAnotherUser() = runServerTest {
        installRouting()

        val creatorClient = createClient(token = adminBearerToken.accessToken)
        creatorClient.post(request = HttpRequest(url = URL.Materials), body = FakeData.material)

        val otherAuth = seedUser(registrationForm = registrationForm(username = "other_material_user", permissions = mapOf(EntityType.MATERIAL to Permission.WRITE)))
        val otherClient = createClient(token = otherAuth.bearer.accessToken)

        val editedMaterial = FakeData.material.copy(name = "Edited")
        assertEquals(expected = HttpStatusCode.OK, actual = otherClient.post(request = HttpRequest(url = URL.Materials), body = editedMaterial).status)
        assertEquals(expected = editedMaterial, actual = otherClient.ktorGet(urlString = "${URL.Materials.path}/${FakeData.material.uuid}").body<Material>())
    }

    /** Verifies that fetching a single material requires authentication, and that upserting requires material write permission. */
    @Test
    fun materialRoutesRequireAuthenticationAndPermission() = runServerTest {
        installRouting()

        val client = createClient(token = null)
        assertEquals(expected = HttpStatusCode.Unauthorized, actual = client.ktorGet(urlString = "${URL.Materials.path}/${FakeData.material.uuid}").status)

        // FakeData.account only has MATERIAL -> READ.
        val userClient = createClient(token = bearerToken.accessToken)
        assertEquals(expected = HttpStatusCode.OK, actual = userClient.get(request = HttpRequest(url = URL.Materials)).status)
        assertEquals(expected = HttpStatusCode.Forbidden, actual = userClient.post(request = HttpRequest(url = URL.Materials), body = FakeData.material).status)
    }

    /** Verifies that reading materials is forbidden for a user without any material permission. */
    @Test
    fun getMaterialsRequiresReadPermission() = runServerTest {
        installRouting()

        val noMaterialAuth = seedUser(registrationForm = registrationForm(username = "no_material_user", permissions = mapOf(EntityType.USER to Permission.READ)))
        val client = createClient(token = noMaterialAuth.bearer.accessToken)

        assertEquals(expected = HttpStatusCode.Forbidden, actual = client.get(request = HttpRequest(url = URL.Materials)).status)
        assertEquals(expected = HttpStatusCode.Forbidden, actual = client.ktorGet(urlString = "${URL.Materials.path}/${FakeData.material.uuid}").status)
    }

    /** Verifies that a database failure while listing materials answers 500 rather than an empty list, so a client never takes it as having nothing new to sync. */
    @Test
    fun getMaterialsDatabaseFailure() = runServerTest {
        installRouting()
        val authenticatedClient = createClient(token = adminBearerToken.accessToken)
        dependency.get().database.reset()

        assertEquals(expected = HttpStatusCode.InternalServerError, actual = authenticatedClient.get(request = HttpRequest(url = URL.Materials)).status)
    }
}
