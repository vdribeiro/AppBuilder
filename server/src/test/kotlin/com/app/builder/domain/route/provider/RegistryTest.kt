package com.app.builder.domain.route.provider

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import io.ktor.client.call.body
import io.ktor.http.HttpStatusCode
import com.app.builder.data.http.HttpRequest
import com.app.builder.data.http.Query
import com.app.builder.data.http.URL
import com.app.builder.data.http.get
import com.app.builder.data.http.post
import com.app.builder.domain.Registry
import com.app.builder.test.FakeData
import com.app.builder.test.TestCase

class RegistryTest: TestCase() {

    /** Verifies that fetching registries requires authentication and read permission, and reflects previously posted registries. */
    @Test
    fun getRegistries() = runServerTest {
        installRouting()

        val client = createClient(token = null)
        assertEquals(expected = HttpStatusCode.Unauthorized, actual = client.get(request = HttpRequest(url = URL.Registries)).status)

        val userClient = createClient(token = bearerToken.accessToken)
        val request = HttpRequest(url = URL.Registries, queryMap = mapOf(Query.LastSyncUtc to "1970-01-01T00:00:00Z"))

        val emptyResponse = userClient.get(request = request)
        assertEquals(expected = HttpStatusCode.OK, actual = emptyResponse.status)
        assertTrue(actual = emptyResponse.body<List<Registry>>().isEmpty())

        val adminClient = createClient(token = adminBearerToken.accessToken)
        adminClient.post(request = HttpRequest(url = URL.Registries), body = FakeData.registry)

        val listResponse = userClient.get(request = request)
        assertEquals(expected = HttpStatusCode.OK, actual = listResponse.status)
        assertEquals(expected = 1, actual = listResponse.body<List<Registry>>().size)
    }

    /** Verifies that upserting a registry requires authentication and write permission, rejects invalid bodies, and returns the saved registry. */
    @Test
    fun upsertRegistry() = runServerTest {
        installRouting()

        val client = createClient(token = null)
        assertEquals(expected = HttpStatusCode.Unauthorized, actual = client.post(request = HttpRequest(url = URL.Registries), body = FakeData.registry).status)

        // FakeData.user only has read permission for the registry entity type.
        val userClient = createClient(token = bearerToken.accessToken)
        assertEquals(expected = HttpStatusCode.Forbidden, actual = userClient.post(request = HttpRequest(url = URL.Registries), body = FakeData.registry).status)

        val adminClient = createClient(token = adminBearerToken.accessToken)
        assertEquals(expected = HttpStatusCode.BadRequest, actual = adminClient.post(request = HttpRequest(url = URL.Registries), body = "invalid body").status)

        val response = adminClient.post(request = HttpRequest(url = URL.Registries), body = FakeData.registry)
        assertEquals(expected = HttpStatusCode.OK, actual = response.status)
        assertEquals(expected = FakeData.registry, actual = response.body<Registry>())
    }
}
