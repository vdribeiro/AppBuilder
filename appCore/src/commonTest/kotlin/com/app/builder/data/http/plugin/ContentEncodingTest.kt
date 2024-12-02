package com.app.builder.data.http.plugin

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.currentCoroutineContext
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.mock.MockEngineConfig
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.get
import io.ktor.http.HttpHeaders
import io.ktor.http.headersOf
import io.ktor.util.GZipEncoder
import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.toByteArray
import com.app.builder.data.http.TestEngine
import com.app.builder.data.http.baseUrl
import com.app.builder.data.http.respondMock
import com.app.builder.test.TestCase

class ContentEncodingTest: TestCase() {

    /** Verifies requests advertise gzip as an accepted content encoding. */
    @Test
    fun advertisesGzipEncoding() = runUnitTest {
        var acceptEncoding: String? = null
        val client = HttpClient(engine = TestEngine(config = MockEngineConfig().apply {
            requestHandlers.add { request ->
                acceptEncoding = request.headers[HttpHeaders.AcceptEncoding]
                respondMock()
            }
        })) {
            installContentEncoding()
        }

        client.get(urlString = baseUrl)

        assertTrue(actual = acceptEncoding.orEmpty().contains(other = "gzip"))
    }

    /** Verifies that a gzip-encoded response body is transparently decompressed into its original content. */
    @Test
    fun decompressesGzipEncodedResponseBody() = runUnitTest {
        val original = "gzip-body-content"
        val compressed = GZipEncoder.encode(source = ByteReadChannel(content = original.encodeToByteArray()), coroutineContext = currentCoroutineContext()).toByteArray()

        val client = HttpClient(engine = TestEngine(config = MockEngineConfig().apply {
            requestHandlers.add {
                respond(
                    content = compressed,
                    headers = headersOf(HttpHeaders.ContentEncoding to listOf("gzip"))
                )
            }
        })) {
            installContentEncoding()
        }

        val response = client.get(urlString = baseUrl)
        val body: String = response.body()

        assertEquals(expected = original, actual = body)
    }
}
