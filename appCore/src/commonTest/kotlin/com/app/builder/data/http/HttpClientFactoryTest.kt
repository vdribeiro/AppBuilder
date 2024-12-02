package com.app.builder.data.http

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import io.ktor.client.engine.mock.MockEngineConfig
import io.ktor.client.request.get
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import com.app.builder.test.TestCase

class HttpClientFactoryTest: TestCase() {

    /**
     * Verifies that the built HTTP client completes a request and that plugin-installed effects are present:
     * the Interceptor plugin's default headers and the ContentEncoding plugin's advertised gzip encoding.
     */
    @Test
    fun buildsHttpClientWithAllPluginsInstalled() = runUnitTest {
        var requestUuid: String? = null
        var acceptEncoding: String? = null
        val factory = HttpClientFactory(engine = TestEngine(config = MockEngineConfig().apply {
            requestHandlers.add { request ->
                requestUuid = request.headers[Header.RequestUuid.header]
                acceptEncoding = request.headers[HttpHeaders.AcceptEncoding]
                respondMock()
            }
        }))

        val response = factory.httpClient.get(urlString = baseUrl)

        assertEquals(expected = HttpStatusCode.OK, actual = response.status)
        assertTrue(actual = requestUuid.orEmpty().isNotBlank())
        assertTrue(actual = acceptEncoding.orEmpty().contains(other = "gzip"))
    }
}
