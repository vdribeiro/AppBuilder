package com.app.builder.data.http.plugin

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngineConfig
import io.ktor.client.plugins.auth.providers.BearerTokens
import io.ktor.client.request.get
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import com.app.builder.data.http.TestEngine
import com.app.builder.data.http.baseUrl
import com.app.builder.data.http.respondMock
import com.app.builder.test.TestCase

class AuthTest: TestCase() {

    /** Verifies the bearer token is attached to requests before any response is received. */
    @Test
    fun attachesBearerTokenPreemptively() = runUnitTest {
        var authorization: String? = null
        val client = HttpClient(engine = TestEngine(config = MockEngineConfig().apply {
            requestHandlers.add { request ->
                authorization = request.headers[HttpHeaders.Authorization]
                respondMock()
            }
        })) {
            installAuth(loadTokens = { BearerTokens(accessToken = "access-token", refreshToken = null) })
        }

        client.get(urlString = baseUrl)

        assertEquals(expected = "Bearer access-token", actual = authorization)
    }

    /** Retries the request with a refreshed token after receiving an unauthorized response. */
    @Test
    fun refreshesTokenAfterUnauthorizedAndRetries() = runUnitTest {
        var attempt = 0
        var refreshInvoked = false
        val client = HttpClient(engine = TestEngine(config = MockEngineConfig().apply {
            requestHandlers.add { request ->
                attempt++
                when (attempt) {
                    1 -> {
                        assertEquals(expected = "Bearer old-token", actual = request.headers[HttpHeaders.Authorization])
                        respondMock(status = HttpStatusCode.Unauthorized)
                    }

                    else -> {
                        assertEquals(expected = "Bearer new-token", actual = request.headers[HttpHeaders.Authorization])
                        respondMock()
                    }
                }
            }
        })) {
            installAuth(
                loadTokens = { BearerTokens(accessToken = "old-token", refreshToken = null) },
                refreshTokens = {
                    refreshInvoked = true
                    BearerTokens(accessToken = "new-token", refreshToken = null)
                }
            )
        }

        val response = client.get(urlString = baseUrl)

        assertEquals(expected = HttpStatusCode.OK, actual = response.status)
        assertTrue(actual = refreshInvoked)
        assertEquals(expected = 2, actual = attempt)
    }

    /** Verifies that no Authorization header is attached when [installAuth]'s `loadTokens` yields no tokens. */
    @Test
    fun noStoredTokensMeansNoAuthorizationHeader() = runUnitTest {
        var authorization: String? = "not-checked-yet"
        val client = HttpClient(engine = TestEngine(config = MockEngineConfig().apply {
            requestHandlers.add { request ->
                authorization = request.headers[HttpHeaders.Authorization]
                respondMock()
            }
        })) {
            installAuth(loadTokens = { null })
        }

        client.get(urlString = baseUrl)

        assertNull(actual = authorization)
    }

    /** Verifies that an unrenewable session, where `refreshTokens` yields no tokens, leaves the original unauthorized response unretried. */
    @Test
    fun unrenewableSessionLeavesUnauthorizedResponseUnretried() = runUnitTest {
        var attempt = 0
        val client = HttpClient(engine = TestEngine(config = MockEngineConfig().apply {
            requestHandlers.add {
                attempt++
                respondMock(status = HttpStatusCode.Unauthorized)
            }
        })) {
            installAuth(
                loadTokens = { BearerTokens(accessToken = "old-token", refreshToken = null) },
                refreshTokens = { null }
            )
        }

        val response = client.get(urlString = baseUrl)

        assertEquals(expected = HttpStatusCode.Unauthorized, actual = response.status)
        assertEquals(expected = 1, actual = attempt)
    }
}
