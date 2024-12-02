package com.app.builder.data.http.plugin

import kotlin.test.Test
import kotlin.test.assertEquals
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.mock.MockEngineConfig
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.get
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.headersOf
import com.app.builder.data.http.TestEngine
import com.app.builder.data.http.baseUrl
import com.app.builder.data.serializer.encode
import com.app.builder.domain.Translation
import com.app.builder.test.TestCase

class ContentNegotiationTest: TestCase() {

    /** Sample translation used as the decoded response body in tests. */
    private val translation = Translation(languageIso = "en", key = "key", value = "value")

    /** Verifies JSON responses are decoded into the expected body. */
    @Test
    fun decodesJsonResponses() = runUnitTest {
        val client = HttpClient(engine = TestEngine(config = MockEngineConfig().apply {
            requestHandlers.add {
                respond(
                    content = encode(value = translation).orEmpty(),
                    headers = headersOf(HttpHeaders.ContentType to listOf(ContentType.Application.Json.toString()))
                )
            }
        })) {
            installContentNegotiation()
        }

        assertEquals(expected = translation, actual = client.get(urlString = baseUrl).body())
    }

    /** Verifies plain-text responses fall back to decoding into the expected body. */
    @Test
    fun decodesPlainTextFallbackResponses() = runUnitTest {
        val client = HttpClient(engine = TestEngine(config = MockEngineConfig().apply {
            requestHandlers.add {
                respond(
                    content = encode(value = translation).orEmpty(),
                    headers = headersOf(HttpHeaders.ContentType to listOf(ContentType.Text.Plain.toString()))
                )
            }
        })) {
            installContentNegotiation()
        }

        assertEquals(expected = translation, actual = client.get(urlString = baseUrl).body())
    }
}
