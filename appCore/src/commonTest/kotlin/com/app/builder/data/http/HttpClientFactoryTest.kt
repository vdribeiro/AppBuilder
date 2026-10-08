package com.app.builder.data.http

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue
import io.ktor.client.engine.mock.MockEngineConfig
import io.ktor.client.plugins.auth.providers.BearerTokens
import io.ktor.client.request.get
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import com.app.builder.data.http.plugin.UnauthorizedException
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

    /**
     * Verifies the assembled client refreshes an expired token and retries, rather than surfacing the 401.
     * This exercises the Interceptor and Auth plugins together, which is the only configuration that catches their ordering:
     * validating the response inside Auth's retry, or installing the Interceptor after it, makes the 401 escape before Auth can refresh,
     * leaving every access token expiry a terminal failure that burns the queued job behind it.
     */
    @Test
    fun refreshesExpiredTokenAndRetriesThroughTheAssembledStack() = runUnitTest {
        var attempt = 0
        var refreshInvoked = false
        val factory = HttpClientFactory(
            engine = TestEngine(config = MockEngineConfig().apply {
                requestHandlers.add {
                    attempt++
                    if (attempt == 1) respondMock(status = HttpStatusCode.Unauthorized) else respondMock()
                }
            }),
            configurations = Configurations(
                loadTokens = { BearerTokens(accessToken = "expired-token", refreshToken = "refresh-token") },
                refreshTokens = {
                    refreshInvoked = true
                    BearerTokens(accessToken = "fresh-token", refreshToken = "refresh-token")
                }
            )
        )

        val response = factory.httpClient.get(urlString = baseUrl)

        assertEquals(expected = HttpStatusCode.OK, actual = response.status)
        assertTrue(actual = refreshInvoked)
        assertEquals(expected = 2, actual = attempt)
    }

    /** Verifies that a session the server will not renew still surfaces as unauthorized once Auth has exhausted its refresh. */
    @Test
    fun unrenewableSessionStillSurfacesUnauthorized() = runUnitTest {
        val factory = HttpClientFactory(
            engine = TestEngine(config = MockEngineConfig().apply {
                requestHandlers.add { respondMock(status = HttpStatusCode.Unauthorized) }
            }),
            configurations = Configurations(
                loadTokens = { BearerTokens(accessToken = "expired-token", refreshToken = "refresh-token") },
                refreshTokens = { null }
            )
        )

        assertFailsWith<UnauthorizedException> { factory.httpClient.get(urlString = baseUrl) }
    }
}
