package com.app.builder.domain.route.provider

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import io.ktor.client.call.body
import io.ktor.http.HttpStatusCode
import com.app.builder.core.config.ClientConfigs
import com.app.builder.core.config.ClientFlags
import com.app.builder.core.config.ServerConfigs
import com.app.builder.core.config.ServerFlags
import com.app.builder.data.http.HttpRequest
import com.app.builder.data.http.URL
import com.app.builder.data.http.get
import com.app.builder.data.http.post
import com.app.builder.domain.EntityType
import com.app.builder.test.FakeData
import com.app.builder.test.TestCase

class ConfigTest: TestCase() {

    /** Verifies that client flags and configs can be fetched without authentication. */
    @Test
    fun getClientConfigs() = runServerTest {
        installRouting()
        val client = createClient(token = null)

        assertEquals(expected = ClientFlags.value, actual = client.get(request = HttpRequest(url = URL.ClientFlags)).body<ClientFlags>())
        assertEquals(expected = ClientConfigs.value, actual = client.get(request = HttpRequest(url = URL.ClientConfigs)).body<ClientConfigs>())
    }

    /** Verifies that updating client flags and configs requires admin permission and persists valid values. */
    @Test
    fun updateClientConfigs() = runServerTest {
        installRouting()
        val client = createClient(token = bearerToken.accessToken)

        assertEquals(expected = HttpStatusCode.Forbidden, actual = client.post(request = HttpRequest(url = URL.ClientFlags), body = ClientFlags.value).status)
        assertEquals(expected = HttpStatusCode.Forbidden, actual = client.post(request = HttpRequest(url = URL.ClientConfigs), body = ClientConfigs.value).status)

        val adminClient = createClient(token = adminBearerToken.accessToken)

        assertEquals(expected = HttpStatusCode.BadRequest, actual = adminClient.post(request = HttpRequest(url = URL.ClientFlags), body = "not a flag").status)

        val flags = ClientFlags.value.copy(http = !ClientFlags.value.scheduler)
        val flagsResponse = adminClient.post(request = HttpRequest(url = URL.ClientFlags), body = flags)
        assertEquals(expected = HttpStatusCode.OK, actual = flagsResponse.status)
        assertEquals(expected = flags, actual = flagsResponse.body<ClientFlags>())
        assertEquals(expected = flags, actual = ClientFlags.value)

        val configs = ClientConfigs.value.copy(httpConnectTimeoutMillis = 1234L)
        val configsResponse = adminClient.post(request = HttpRequest(url = URL.ClientConfigs), body = configs)
        assertEquals(expected = HttpStatusCode.OK, actual = configsResponse.status)
        assertEquals(expected = configs, actual = configsResponse.body<ClientConfigs>())
        assertEquals(expected = configs, actual = ClientConfigs.value)
    }

    /** Verifies that reading server flags and configs requires admin permission. */
    @Test
    fun getServerConfigs() = runServerTest {
        installRouting()
        val client = createClient(token = bearerToken.accessToken)

        assertEquals(expected = HttpStatusCode.Forbidden, actual = client.get(request = HttpRequest(url = URL.ServerFlags)).status)
        assertEquals(expected = HttpStatusCode.Forbidden, actual = client.get(request = HttpRequest(url = URL.ServerConfigs)).status)

        val adminClient = createClient(token = adminBearerToken.accessToken)

        assertEquals(expected = ServerFlags.value, actual = adminClient.get(request = HttpRequest(url = URL.ServerFlags)).body<ServerFlags>())
        assertEquals(expected = ServerConfigs.value, actual = adminClient.get(request = HttpRequest(url = URL.ServerConfigs)).body<ServerConfigs>())
    }

    /** Verifies that reading server flags and configs is recorded in the registry. */
    @Test
    fun getServerConfigsIsRecorded() = runServerTest {
        installRouting()
        val adminClient = createClient(token = adminBearerToken.accessToken)

        assertEquals(expected = HttpStatusCode.OK, actual = adminClient.get(request = HttpRequest(url = URL.ServerFlags, headerMap = registryHeaders())).status)
        assertEquals(expected = HttpStatusCode.OK, actual = adminClient.get(request = HttpRequest(url = URL.ServerConfigs, headerMap = registryHeaders())).status)

        awaitUntil { registries().size == 2 }
        val registries = registries()
        assertTrue(actual = registries.all { it.requestType == "GET" && it.userUuid == FakeData.adminUser.uuid })
        assertEquals(expected = setOf(EntityType.SERVER_FLAG, EntityType.SERVER_CONFIG), actual = registries.map { it.entityType }.toSet())
    }

    /** Verifies that updating server flags and configs requires admin permission and persists valid values. */
    @Test
    fun updateServerConfigs() = runServerTest {
        installRouting()
        val client = createClient(token = bearerToken.accessToken)

        assertEquals(expected = HttpStatusCode.Forbidden, actual = client.post(request = HttpRequest(url = URL.ServerFlags), body = ServerFlags.value).status)
        assertEquals(expected = HttpStatusCode.Forbidden, actual = client.post(request = HttpRequest(url = URL.ServerConfigs), body = ServerConfigs.value).status)

        val adminClient = createClient(token = adminBearerToken.accessToken)

        assertEquals(expected = HttpStatusCode.BadRequest, actual = adminClient.post(request = HttpRequest(url = URL.ServerConfigs), body = "not a config").status)

        val flags = ServerFlags.value.copy(firebase = !ServerFlags.value.firebase)
        val flagsResponse = adminClient.post(request = HttpRequest(url = URL.ServerFlags), body = flags)
        assertEquals(expected = HttpStatusCode.OK, actual = flagsResponse.status)
        assertEquals(expected = flags, actual = flagsResponse.body<ServerFlags>())
        assertEquals(expected = flags, actual = ServerFlags.value)

        val configs = ServerConfigs.value.copy(accessTokenValidity = 4321L)
        val configsResponse = adminClient.post(request = HttpRequest(url = URL.ServerConfigs), body = configs)
        assertEquals(expected = HttpStatusCode.OK, actual = configsResponse.status)
        assertEquals(expected = configs, actual = configsResponse.body<ServerConfigs>())
        assertEquals(expected = configs, actual = ServerConfigs.value)
    }

    /** Verifies that unauthenticated requests to the protected server flags and configs endpoints are rejected. */
    @Test
    fun unauthenticatedServerConfigsAreRejected() = runServerTest {
        installRouting()
        val client = createClient(token = null)

        assertEquals(expected = HttpStatusCode.Unauthorized, actual = client.get(request = HttpRequest(url = URL.ServerFlags)).status)
        assertEquals(expected = HttpStatusCode.Unauthorized, actual = client.get(request = HttpRequest(url = URL.ServerConfigs)).status)
        assertEquals(expected = HttpStatusCode.Unauthorized, actual = client.post(request = HttpRequest(url = URL.ServerFlags), body = ServerFlags.value).status)
        assertEquals(expected = HttpStatusCode.Unauthorized, actual = client.post(request = HttpRequest(url = URL.ServerConfigs), body = ServerConfigs.value).status)
    }

    /** Verifies that unauthenticated requests to update client flags and configs are rejected. */
    @Test
    fun unauthenticatedClientConfigUpdatesAreRejected() = runServerTest {
        installRouting()
        val client = createClient(token = null)

        assertEquals(expected = HttpStatusCode.Unauthorized, actual = client.post(request = HttpRequest(url = URL.ClientFlags), body = ClientFlags.value).status)
        assertEquals(expected = HttpStatusCode.Unauthorized, actual = client.post(request = HttpRequest(url = URL.ClientConfigs), body = ClientConfigs.value).status)
    }
}
