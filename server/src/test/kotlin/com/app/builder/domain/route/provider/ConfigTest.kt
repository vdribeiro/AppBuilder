package com.app.builder.domain.route.provider

import kotlin.test.Test
import kotlin.test.assertEquals
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
import com.app.builder.test.FakeData
import com.app.builder.test.TestCase

class ConfigTest: TestCase() {

    /** Verifies that client flags and configs can be fetched without authentication. */
    @Test
    fun getClientConfigs() = runServerTest {
        installRouting()
        val client = createClient(token = null)

        assertEquals(expected = FakeData.clientFlags, actual = client.get(request = HttpRequest(url = URL.ClientFlags)).body<ClientFlags>())
        assertEquals(expected = FakeData.clientConfigs, actual = client.get(request = HttpRequest(url = URL.ClientConfigs)).body<ClientConfigs>())
    }

    /** Verifies that updating client flags and configs requires admin permission and persists valid values. */
    @Test
    fun updateClientConfigs() = runServerTest {
        installRouting()
        val client = createClient(token = bearerToken.accessToken)

        assertEquals(expected = HttpStatusCode.Forbidden, actual = client.post(request = HttpRequest(url = URL.ClientFlags), body = FakeData.clientFlags).status)
        assertEquals(expected = HttpStatusCode.Forbidden, actual = client.post(request = HttpRequest(url = URL.ClientConfigs), body = FakeData.clientConfigs).status)

        val adminClient = createClient(token = adminBearerToken.accessToken)

        assertEquals(expected = HttpStatusCode.BadRequest, actual = adminClient.post(request = HttpRequest(url = URL.ClientFlags), body = "not a flag").status)

        val flags = FakeData.clientFlags.copy(http = !FakeData.clientFlags.scheduler)
        val flagsResponse = adminClient.post(request = HttpRequest(url = URL.ClientFlags), body = flags)
        assertEquals(expected = HttpStatusCode.OK, actual = flagsResponse.status)
        assertEquals(expected = flags, actual = flagsResponse.body<ClientFlags>())
        assertEquals(expected = flags, actual = ClientFlags.flags)

        val configs = FakeData.clientConfigs.copy(httpConnectTimeoutMillis = 1234L)
        val configsResponse = adminClient.post(request = HttpRequest(url = URL.ClientConfigs), body = configs)
        assertEquals(expected = HttpStatusCode.OK, actual = configsResponse.status)
        assertEquals(expected = configs, actual = configsResponse.body<ClientConfigs>())
        assertEquals(expected = configs, actual = ClientConfigs.configs)
    }

    /** Verifies that reading server flags and configs requires admin permission. */
    @Test
    fun getServerConfigs() = runServerTest {
        installRouting()
        val client = createClient(token = bearerToken.accessToken)

        assertEquals(expected = HttpStatusCode.Forbidden, actual = client.get(request = HttpRequest(url = URL.ServerFlags)).status)
        assertEquals(expected = HttpStatusCode.Forbidden, actual = client.get(request = HttpRequest(url = URL.ServerConfigs)).status)

        val adminClient = createClient(token = adminBearerToken.accessToken)

        assertEquals(expected = FakeData.serverFlags, actual = adminClient.get(request = HttpRequest(url = URL.ServerFlags)).body<ServerFlags>())
        assertEquals(expected = FakeData.serverConfigs, actual = adminClient.get(request = HttpRequest(url = URL.ServerConfigs)).body<ServerConfigs>())
    }

    /** Verifies that updating server flags and configs requires admin permission and persists valid values. */
    @Test
    fun updateServerConfigs() = runServerTest {
        installRouting()
        val client = createClient(token = bearerToken.accessToken)

        assertEquals(expected = HttpStatusCode.Forbidden, actual = client.post(request = HttpRequest(url = URL.ServerFlags), body = FakeData.serverFlags).status)
        assertEquals(expected = HttpStatusCode.Forbidden, actual = client.post(request = HttpRequest(url = URL.ServerConfigs), body = FakeData.serverConfigs).status)

        val adminClient = createClient(token = adminBearerToken.accessToken)

        assertEquals(expected = HttpStatusCode.BadRequest, actual = adminClient.post(request = HttpRequest(url = URL.ServerConfigs), body = "not a config").status)

        val flags = FakeData.serverFlags.copy(firebase = !FakeData.serverFlags.firebase)
        val flagsResponse = adminClient.post(request = HttpRequest(url = URL.ServerFlags), body = flags)
        assertEquals(expected = HttpStatusCode.OK, actual = flagsResponse.status)
        assertEquals(expected = flags, actual = flagsResponse.body<ServerFlags>())
        assertEquals(expected = flags, actual = ServerFlags.flags)

        val configs = FakeData.serverConfigs.copy(accessTokenValidity = 4321L)
        val configsResponse = adminClient.post(request = HttpRequest(url = URL.ServerConfigs), body = configs)
        assertEquals(expected = HttpStatusCode.OK, actual = configsResponse.status)
        assertEquals(expected = configs, actual = configsResponse.body<ServerConfigs>())
        assertEquals(expected = configs, actual = ServerConfigs.configs)
    }

    /** Verifies that unauthenticated requests to the protected server flags and configs endpoints are rejected. */
    @Test
    fun unauthenticatedServerConfigsAreRejected() = runServerTest {
        installRouting()
        val client = createClient(token = null)

        assertEquals(expected = HttpStatusCode.Unauthorized, actual = client.get(request = HttpRequest(url = URL.ServerFlags)).status)
        assertEquals(expected = HttpStatusCode.Unauthorized, actual = client.get(request = HttpRequest(url = URL.ServerConfigs)).status)
        assertEquals(expected = HttpStatusCode.Unauthorized, actual = client.post(request = HttpRequest(url = URL.ServerFlags), body = FakeData.serverFlags).status)
        assertEquals(expected = HttpStatusCode.Unauthorized, actual = client.post(request = HttpRequest(url = URL.ServerConfigs), body = FakeData.serverConfigs).status)
    }
}
