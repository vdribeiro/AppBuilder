package com.app.builder.domain.route.provider

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import io.ktor.client.call.body
import io.ktor.http.HttpStatusCode
import com.app.builder.core.config.ServerFlags
import com.app.builder.data.database.reset
import com.app.builder.data.http.HttpRequest
import com.app.builder.data.http.Query
import com.app.builder.data.http.URL
import com.app.builder.data.http.get
import com.app.builder.data.http.post
import com.app.builder.domain.BuildingMaterial
import com.app.builder.domain.EntityType
import com.app.builder.domain.Permission
import com.app.builder.test.FakeData
import com.app.builder.test.TestCase
import io.ktor.client.request.get as ktorGet

class BuildingMaterialTest: TestCase() {

    override suspend fun beforeTest() {
        super.beforeTest()
        ServerFlags.set { it.copy(buildingMaterials = true) }
    }

    /** Verifies that fetching building materials requires authentication and returns the posted building materials. */
    @Test
    fun getBuildingMaterials() = runServerTest {
        installRouting()

        val client = createClient(token = null)
        assertEquals(expected = HttpStatusCode.Unauthorized, actual = client.get(request = HttpRequest(url = URL.BuildingMaterials)).status)

        val authenticatedClient = createClient(token = adminBearerToken.accessToken)
        val linksRequest = HttpRequest(url = URL.BuildingMaterials, queryMap = mapOf(Query.LastSyncUtc to "1970-01-01T00:00:00Z"))

        val emptyResponse = authenticatedClient.get(request = linksRequest)
        assertEquals(expected = HttpStatusCode.OK, actual = emptyResponse.status)
        assertTrue(actual = emptyResponse.body<List<BuildingMaterial>>().isEmpty())

        assertEquals(expected = HttpStatusCode.OK, actual = authenticatedClient.post(request = HttpRequest(url = URL.BuildingMaterials), body = FakeData.buildingMaterial).status)
        assertEquals(expected = listOf(element = FakeData.buildingMaterial), actual = authenticatedClient.get(request = linksRequest).body<List<BuildingMaterial>>())
        assertEquals(expected = FakeData.buildingMaterial, actual = authenticatedClient.ktorGet(urlString = "${URL.BuildingMaterials.path}/${FakeData.buildingMaterial.uuid}").body<BuildingMaterial>())
    }

    /** Verifies that reading building materials requires the building material read permission and writing them its write permission, whatever the building and material permissions are. */
    @Test
    fun permissions() = runServerTest {
        installRouting()
        val request = HttpRequest(url = URL.BuildingMaterials)

        val cases = listOf(
            "buildings_and_materials" to mapOf(EntityType.BUILDING to Permission.WRITE, EntityType.MATERIAL to Permission.WRITE) to (HttpStatusCode.Forbidden to HttpStatusCode.Forbidden),
            "link_reader" to mapOf(EntityType.BUILDING_MATERIAL to Permission.READ) to (HttpStatusCode.OK to HttpStatusCode.Forbidden),
            "link_writer" to mapOf(EntityType.BUILDING_MATERIAL to Permission.WRITE) to (HttpStatusCode.OK to HttpStatusCode.OK)
        )
        cases.forEach { (user, expected) ->
            val (username, permissions) = user
            val auth = seedUser(registrationForm = registrationForm(username = username, permissions = permissions))
            val client = createClient(token = auth.bearer.accessToken)
            val actual = client.get(request = request).status to client.post(request = request, body = FakeData.buildingMaterial).status
            assertEquals(expected = expected, actual = actual, message = username)
        }
    }

    /** Verifies that a building material is accepted with a quantity of at least one, replacing the stored quantity, and rejected below that. */
    @Test
    fun upsertBuildingMaterialValidatesQuantity() = runServerTest {
        installRouting()
        val client = createClient(token = adminBearerToken.accessToken)
        val request = HttpRequest(url = URL.BuildingMaterials)

        assertEquals(expected = HttpStatusCode.OK, actual = client.post(request = request, body = FakeData.buildingMaterial.copy(quantity = 1)).status)
        assertEquals(expected = 1, actual = client.ktorGet(urlString = "${URL.BuildingMaterials.path}/${FakeData.buildingMaterial.uuid}").body<BuildingMaterial>().quantity)
        listOf(0, -1).forEach {
            assertEquals(expected = HttpStatusCode.BadRequest, actual = client.post(request = request, body = FakeData.buildingMaterial.copy(quantity = it)).status, message = "$it")
        }
        assertEquals(expected = 1, actual = client.ktorGet(urlString = "${URL.BuildingMaterials.path}/${FakeData.buildingMaterial.uuid}").body<BuildingMaterial>().quantity)
    }

    /** Verifies that a database failure while listing building materials answers 500. */
    @Test
    fun getBuildingMaterialsDatabaseFailure() = runServerTest {
        installRouting()
        val authenticatedClient = createClient(token = adminBearerToken.accessToken)
        dependency.get().database.reset()

        assertEquals(expected = HttpStatusCode.InternalServerError, actual = authenticatedClient.get(request = HttpRequest(url = URL.BuildingMaterials)).status)
    }
}
