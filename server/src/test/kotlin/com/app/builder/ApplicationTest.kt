package com.app.builder

import kotlin.test.Test
import kotlin.test.assertEquals
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import com.app.builder.test.TestCase

class ApplicationTest: TestCase() {

    /** Verifies the root endpoint responds with the running status message. */
    @Test
    fun app() = runServerTest {
        application { module() }

        assertEquals(expected = "Server running", actual = client.get(urlString = "/").bodyAsText())
    }
}
