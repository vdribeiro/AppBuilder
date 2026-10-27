package com.app.builder.data.http

import kotlin.test.Test
import kotlin.test.assertEquals
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import com.app.builder.test.TestCase

class NoOpHttpClientEngineTest: TestCase() {

    /** Verifies that every request through [NoOpHttpClientEngine] answers 204 with an empty body. */
    @Test
    fun respondsNoContent() = runUnitTest {
        val response = HttpClient(engine = NoOpHttpClientEngine).get(urlString = "https://example.com")

        assertEquals(expected = HttpStatusCode.NoContent, actual = response.status)
        assertEquals(expected = "", actual = response.bodyAsText())
    }
}
