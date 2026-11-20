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
import com.app.builder.domain.EntityType
import com.app.builder.domain.EquipmentUser
import com.app.builder.domain.Permission
import com.app.builder.test.FakeData
import com.app.builder.test.TestCase
import io.ktor.client.request.get as ktorGet

class EquipmentUserTest: TestCase() {

    override suspend fun beforeTest() {
        super.beforeTest()
        ServerFlags.set { it.copy(equipmentUsers = true) }
    }

    /** Verifies that fetching equipment users requires authentication and returns the posted equipment users. */
    @Test
    fun getEquipmentUsers() = runServerTest {
        installRouting()

        val client = createClient(token = null)
        assertEquals(expected = HttpStatusCode.Unauthorized, actual = client.get(request = HttpRequest(url = URL.EquipmentUsers)).status)

        val authenticatedClient = createClient(token = adminBearerToken.accessToken)
        val linksRequest = HttpRequest(url = URL.EquipmentUsers, queryMap = mapOf(Query.LastSyncUtc to "1970-01-01T00:00:00Z"))

        val emptyResponse = authenticatedClient.get(request = linksRequest)
        assertEquals(expected = HttpStatusCode.OK, actual = emptyResponse.status)
        assertTrue(actual = emptyResponse.body<List<EquipmentUser>>().isEmpty())

        assertEquals(expected = HttpStatusCode.OK, actual = authenticatedClient.post(request = HttpRequest(url = URL.EquipmentUsers), body = FakeData.equipmentUser).status)
        assertEquals(expected = listOf(element = FakeData.equipmentUser), actual = authenticatedClient.get(request = linksRequest).body<List<EquipmentUser>>())
        assertEquals(expected = FakeData.equipmentUser, actual = authenticatedClient.ktorGet(urlString = "${URL.EquipmentUsers.path}/${FakeData.equipmentUser.uuid}").body<EquipmentUser>())
    }

    /** Verifies that reading equipment users requires the equipment user read permission and writing them its write permission, whatever the equipment and user permissions are. */
    @Test
    fun permissions() = runServerTest {
        installRouting()
        val request = HttpRequest(url = URL.EquipmentUsers)

        val cases = listOf(
            "equipments_and_users" to mapOf(EntityType.EQUIPMENT to Permission.WRITE, EntityType.USER to Permission.WRITE) to (HttpStatusCode.Forbidden to HttpStatusCode.Forbidden),
            "link_reader" to mapOf(EntityType.EQUIPMENT_USER to Permission.READ) to (HttpStatusCode.OK to HttpStatusCode.Forbidden),
            "link_writer" to mapOf(EntityType.EQUIPMENT_USER to Permission.WRITE) to (HttpStatusCode.OK to HttpStatusCode.OK)
        )
        cases.forEach { (user, expected) ->
            val (username, permissions) = user
            val auth = seedUser(registrationForm = registrationForm(username = username, permissions = permissions))
            val client = createClient(token = auth.bearer.accessToken)
            val actual = client.get(request = request).status to client.post(request = request, body = FakeData.equipmentUser).status
            assertEquals(expected = expected, actual = actual, message = username)
        }
    }

    /** Verifies that a database failure while listing equipment users answers 500. */
    @Test
    fun getEquipmentUsersDatabaseFailure() = runServerTest {
        installRouting()
        val authenticatedClient = createClient(token = adminBearerToken.accessToken)
        dependency.get().database.reset()

        assertEquals(expected = HttpStatusCode.InternalServerError, actual = authenticatedClient.get(request = HttpRequest(url = URL.EquipmentUsers)).status)
    }
}
