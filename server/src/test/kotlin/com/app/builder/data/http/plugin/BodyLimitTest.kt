package com.app.builder.data.http.plugin

import kotlin.test.Test
import kotlin.test.assertEquals
import io.ktor.http.HttpStatusCode
import com.app.builder.core.config.ServerConfigs
import com.app.builder.core.config.ServerFlags
import com.app.builder.data.http.HttpRequest
import com.app.builder.data.http.URL
import com.app.builder.data.http.post
import com.app.builder.domain.UserCredentials
import com.app.builder.test.TestCase

class BodyLimitTest: TestCase() {

    override suspend fun beforeTest() {
        super.beforeTest()
        ServerFlags.set { it.copy(authentication = true) }
    }

    /** Verifies that a 16 MiB request body is refused as too large. */
    @Test
    fun oversizedBodyIsRejected() = runServerTest {
        installRouting()
        val client = createClient(token = null)
        val credentials = UserCredentials(username = "unknown", password = "a".repeat(n = 16 * 1024 * 1024))

        assertEquals(expected = HttpStatusCode.PayloadTooLarge, actual = client.post(request = HttpRequest(url = URL.Login), body = credentials).status)
    }

    /** Verifies that the limit follows [ServerConfigs.maxBodySize] while the server runs. */
    @Test
    fun followsMaxBodySize() = runServerTest {
        installRouting()
        val client = createClient(token = null)
        val credentials = UserCredentials(username = "unknown", password = "wrong_password")

        assertEquals(expected = HttpStatusCode.Unauthorized, actual = client.post(request = HttpRequest(url = URL.Login), body = credentials).status)
        ServerConfigs.set { it.copy(maxBodySize = 8L) }
        assertEquals(expected = HttpStatusCode.PayloadTooLarge, actual = client.post(request = HttpRequest(url = URL.Login), body = credentials).status)
    }
}
