package com.app.builder.domain.route.provider

import kotlin.test.Test
import kotlin.test.assertEquals
import io.ktor.client.request.head
import io.ktor.http.HttpStatusCode
import com.app.builder.core.config.ServerConfigs
import com.app.builder.core.config.ServerFlags
import com.app.builder.data.http.URL
import com.app.builder.test.TestCase

class ProbeTest: TestCase() {

    /** Verifies that the probe endpoint responds OK when enabled. */
    @Test
    fun probe() = runServerTest {
        installRouting()
        val client = createClient(token = null)

        assertEquals(expected = HttpStatusCode.OK, actual = client.head(urlString = URL.Probe.path).status)
    }

    /** Verifies that the probe endpoint responds not found when disabled by flag. */
    @Test
    fun probeIsDisabledByFlag() = runServerTest {
        ServerFlags.set { it.copy(probe = false) }
        installRouting()
        val client = createClient(token = null)

        assertEquals(expected = HttpStatusCode.NotFound, actual = client.head(urlString = URL.Probe.path).status)
    }

    /** Verifies that the probe endpoint returns too many requests once the rate limit is exceeded. */
    @Test
    fun probeIsRateLimited() = runServerTest {
        installRouting()
        val client = createClient(token = bearerToken.accessToken)

        repeat(times = ServerConfigs.configs.rateLimiterProbeLimit) {
            assertEquals(expected = HttpStatusCode.OK, actual = client.head(urlString = URL.Probe.path).status)
        }

        assertEquals(expected = HttpStatusCode.TooManyRequests, actual = client.head(urlString = URL.Probe.path).status)
    }
}
