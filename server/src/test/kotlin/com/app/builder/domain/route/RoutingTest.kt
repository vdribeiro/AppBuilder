package com.app.builder.domain.route

import kotlin.test.Test
import kotlin.test.assertEquals
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import com.app.builder.test.TestCase

class RoutingTest: TestCase() {

    /** Verifies that the root endpoint responds with the running status message. */
    @Test
    fun root() = runServerTest {
        installRouting()
        val client = createClient(token = null)
        assertEquals(expected = "Server running", actual = client.get(urlString = "/").bodyAsText())
    }
}
