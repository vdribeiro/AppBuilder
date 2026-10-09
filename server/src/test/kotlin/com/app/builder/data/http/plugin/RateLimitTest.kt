package com.app.builder.data.http.plugin

import kotlin.test.Test
import kotlin.test.assertEquals
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.install
import io.ktor.server.plugins.ratelimit.RateLimit
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import com.app.builder.core.config.ServerConfigs
import com.app.builder.test.TestCase

class RateLimitTest: TestCase() {

    /** Verifies that the header is ignored entirely when no proxy is trusted, so a caller reaching the server directly cannot mint a bucket of its own. */
    @Test
    fun resolveRequestKeyIgnoresTheHeaderWithoutATrustedProxy() = runUnitTest {
        assertEquals(expected = "10.0.0.1", actual = resolveRequestKey(forwardedFor = "1.2.3.4", trustedProxyHops = 0, remoteAddress = "10.0.0.1"))
        assertEquals(expected = "10.0.0.1", actual = resolveRequestKey(forwardedFor = "1.2.3.4", trustedProxyHops = -1, remoteAddress = "10.0.0.1"))
    }

    /** Verifies that a single trusted proxy resolves to the rightmost entry, which is the only one the caller cannot write. */
    @Test
    fun resolveRequestKeyTakesWhatTheNearestProxySaw() = runUnitTest {
        assertEquals(expected = "1.2.3.4", actual = resolveRequestKey(forwardedFor = "1.2.3.4", trustedProxyHops = 1, remoteAddress = "10.0.0.1"))
        // Anything the caller forges is pushed left by the proxy appending the peer it actually saw.
        assertEquals(expected = "1.2.3.4", actual = resolveRequestKey(forwardedFor = "9.9.9.9, 1.2.3.4", trustedProxyHops = 1, remoteAddress = "10.0.0.1"))
        assertEquals(expected = "1.2.3.4", actual = resolveRequestKey(forwardedFor = "evil, 8.8.8.8, 1.2.3.4", trustedProxyHops = 1, remoteAddress = "10.0.0.1"))
    }

    /** Verifies that a second trusted proxy steps one further left, so a CDN in front of the gateway still resolves the real caller. */
    @Test
    fun resolveRequestKeyStepsBackOnePerTrustedHop() = runUnitTest {
        assertEquals(expected = "1.2.3.4", actual = resolveRequestKey(forwardedFor = "1.2.3.4, 172.16.0.9", trustedProxyHops = 2, remoteAddress = "10.0.0.1"))
        assertEquals(expected = "1.2.3.4", actual = resolveRequestKey(forwardedFor = "evil, 1.2.3.4, 172.16.0.9", trustedProxyHops = 2, remoteAddress = "10.0.0.1"))
    }

    /** Verifies that anything unusable falls back to the socket address rather than keying on whatever the caller supplied. */
    @Test
    fun resolveRequestKeyFallsBackToTheSocketAddress() = runUnitTest {
        assertEquals(expected = "10.0.0.1", actual = resolveRequestKey(forwardedFor = null, trustedProxyHops = 1, remoteAddress = "10.0.0.1"))
        assertEquals(expected = "10.0.0.1", actual = resolveRequestKey(forwardedFor = "", trustedProxyHops = 1, remoteAddress = "10.0.0.1"))
        assertEquals(expected = "10.0.0.1", actual = resolveRequestKey(forwardedFor = " , , ", trustedProxyHops = 1, remoteAddress = "10.0.0.1"))
        // A chain shorter than the configured hops means the topology is not what was declared, so the header is not used.
        assertEquals(expected = "10.0.0.1", actual = resolveRequestKey(forwardedFor = "1.2.3.4", trustedProxyHops = 2, remoteAddress = "10.0.0.1"))
    }

    /** Verifies the whole point of the fix: behind a proxy two callers get separate buckets, instead of sharing the one the proxy's own address would key. */
    @Test
    fun callersBehindAProxyAreLimitedSeparately() = runServerTest {
        // Lowered so the test spends its time on the assertion rather than on exhausting the production limit. Reset by the harness after each test.
        ServerConfigs.set { it.copy(rateLimiterLimit = 3) }
        application {
            installRateLimit(trustedProxyHops = 1)
            routing { get(path = "/test") { call.respondText(text = "ok") } }
        }
        val client = createClient(token = null)
        val limit = ServerConfigs.configs.rateLimiterLimit

        repeat(times = limit) {
            assertEquals(expected = HttpStatusCode.OK, actual = client.get(urlString = "/test") { header(HttpHeaders.XForwardedFor, "1.1.1.1") }.status)
        }
        assertEquals(expected = HttpStatusCode.TooManyRequests, actual = client.get(urlString = "/test") { header(HttpHeaders.XForwardedFor, "1.1.1.1") }.status)

        // The second caller arrives over the same proxy connection, and is unaffected by the first exhausting its bucket.
        assertEquals(expected = HttpStatusCode.OK, actual = client.get(urlString = "/test") { header(HttpHeaders.XForwardedFor, "2.2.2.2") }.status)
    }
}
