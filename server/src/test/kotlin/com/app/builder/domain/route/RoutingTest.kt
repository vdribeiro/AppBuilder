package com.app.builder.domain.route

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import com.app.builder.data.http.HttpRequest
import com.app.builder.data.http.URL
import com.app.builder.data.http.get
import com.app.builder.domain.Translation
import com.app.builder.test.TestCase

class RoutingTest: TestCase() {

    /** Verifies that the root endpoint responds with the running status message. */
    @Test
    fun root() = runServerTest {
        installRouting()
        val client = createClient(token = null)
        assertEquals(expected = "Server running", actual = client.get(urlString = "/").bodyAsText())
    }

    /** Verifies that the translations resource endpoint returns a non-empty list. */
    @Test
    fun resources() = runServerTest {
        installRouting()
        val client = createClient(token = null)
        assertTrue(actual = client.get(request = HttpRequest(url = URL.Translations)).body<List<Translation>>().isNotEmpty())
    }
}
