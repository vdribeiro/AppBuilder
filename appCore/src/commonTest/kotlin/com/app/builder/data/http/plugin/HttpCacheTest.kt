package com.app.builder.data.http.plugin

import kotlin.test.Test
import kotlin.test.assertEquals
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngineConfig
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.get
import io.ktor.http.HttpHeaders
import io.ktor.http.headersOf
import com.app.builder.data.http.TestEngine
import com.app.builder.data.http.baseUrl
import com.app.builder.test.TestCase

class HttpCacheTest: TestCase() {

    /** Verifies a second identical request is served from the cache instead of hitting the network. */
    @Test
    fun servesSecondRequestFromCache() = runUnitTest {
        var callCount = 0
        val client = HttpClient(engine = TestEngine(config = MockEngineConfig().apply {
            requestHandlers.add {
                callCount++
                respond(
                    content = "cached",
                    headers = headersOf(HttpHeaders.CacheControl to listOf("max-age=60"))
                )
            }
        })) {
            installHttpCache()
        }

        client.get(urlString = "$baseUrl/cache")
        client.get(urlString = "$baseUrl/cache")

        assertEquals(expected = 1, actual = callCount)
    }

    /** Verifies that a response without a `Cache-Control` header is not cached, and every request hits the network. */
    @Test
    fun doesNotCacheResponseWithoutCacheControlHeader() = runUnitTest {
        var callCount = 0
        val client = HttpClient(engine = TestEngine(config = MockEngineConfig().apply {
            requestHandlers.add {
                callCount++
                respond(content = "not-cached")
            }
        })) {
            installHttpCache()
        }

        client.get(urlString = "$baseUrl/no-cache-control")
        client.get(urlString = "$baseUrl/no-cache-control")

        assertEquals(expected = 2, actual = callCount)
    }

    /** Verifies that a response with a `no-store` directive is not cached. */
    @Test
    fun doesNotCacheResponseWithNoStoreDirective() = runUnitTest {
        var callCount = 0
        val client = HttpClient(engine = TestEngine(config = MockEngineConfig().apply {
            requestHandlers.add {
                callCount++
                respond(
                    content = "not-cached",
                    headers = headersOf(HttpHeaders.CacheControl to listOf("no-store"))
                )
            }
        })) {
            installHttpCache()
        }

        client.get(urlString = "$baseUrl/no-store")
        client.get(urlString = "$baseUrl/no-store")

        assertEquals(expected = 2, actual = callCount)
    }
}
