package com.app.builder.data.http.plugin

import kotlin.time.Duration.Companion.milliseconds
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.install
import io.ktor.server.plugins.origin
import io.ktor.server.plugins.ratelimit.RateLimit
import io.ktor.server.plugins.ratelimit.RateLimitName
import com.app.builder.core.config.ServerConfigs

/** Installs and configures the [RateLimit] plugin for the Ktor [Application]. */
fun Application.installRateLimit() {
    install(plugin = RateLimit) {
        global {
            rateLimiter(limit = ServerConfigs.configs.rateLimiterLimit, refillPeriod = ServerConfigs.configs.rateLimiterRefillPeriod.milliseconds)
            requestKey { call -> call.getRequestKey() }
        }
        register(name = RateLimitName(name = RATE_LIMIT_PROBE)) {
            rateLimiter(limit = ServerConfigs.configs.rateLimiterProbeLimit, refillPeriod = ServerConfigs.configs.rateLimiterProbeRefillPeriod.milliseconds)
            requestKey { call -> call.getRequestKey() }
        }
        register(name = RateLimitName(name = RATE_LIMIT_BROADCAST)) {
            rateLimiter(limit = ServerConfigs.configs.rateLimiterBroadcastLimit, refillPeriod = ServerConfigs.configs.rateLimiterBroadcastRefillPeriod.milliseconds)
            requestKey { call -> call.getRequestKey() }
        }
    }
}

/** Retrieves the request key for the current [ApplicationCall]. */
private fun ApplicationCall.getRequestKey() = request.origin.remoteAddress

/*** Name of the probe rate-limiting. */
const val RATE_LIMIT_PROBE = "probe"
/*** Name of the broadcast rate-limiting. */
const val RATE_LIMIT_BROADCAST = "broadcast"
