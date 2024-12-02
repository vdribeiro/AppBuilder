package com.app.builder.domain.route.provider

import io.ktor.http.HttpStatusCode
import io.ktor.server.plugins.ratelimit.RateLimitName
import io.ktor.server.plugins.ratelimit.rateLimit
import io.ktor.server.routing.Route
import io.ktor.server.routing.head
import com.app.builder.core.telemetry.Telemetry
import com.app.builder.data.http.URL
import com.app.builder.data.http.plugin.RATE_LIMIT_PROBE
import com.app.builder.domain.route.respondSafely

/** Configures the probe route. This is a public endpoint protected by per-IP rate limiting. */
fun Route.probeRoutes() {
    rateLimit(configuration = RateLimitName(name = RATE_LIMIT_PROBE)) {
        head(path = URL.Probe.path) {
            call.respondSafely(status = HttpStatusCode.OK, message = HttpStatusCode.OK)
            Telemetry.info(tag = TAG, message = "Successful probe request")
        }
    }
}

private const val TAG = "ProbeRoute"
