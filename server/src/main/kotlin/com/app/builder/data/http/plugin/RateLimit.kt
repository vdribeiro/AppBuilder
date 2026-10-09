package com.app.builder.data.http.plugin

import kotlin.time.Duration.Companion.milliseconds
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationCall
import io.ktor.http.HttpHeaders
import io.ktor.server.application.install
import io.ktor.server.plugins.origin
import io.ktor.server.plugins.ratelimit.RateLimit
import io.ktor.server.plugins.ratelimit.RateLimitName
import com.app.builder.core.config.ServerConfigs
import com.app.builder.core.platform.Env

/**
 * Installs and configures the [RateLimit] plugin for the Ktor [Application].
 *
 * @param trustedProxyHops How many reverse proxies sit in front of this server, defaulting to [Env.trustedProxyHops].
 */
fun Application.installRateLimit(trustedProxyHops: Int = Env.trustedProxyHops) {
    install(plugin = RateLimit) {
        global {
            rateLimiter(limit = ServerConfigs.configs.rateLimiterLimit, refillPeriod = ServerConfigs.configs.rateLimiterRefillPeriod.milliseconds)
            requestKey { call -> call.getRequestKey(trustedProxyHops = trustedProxyHops) }
        }
        register(name = RateLimitName(name = RATE_LIMIT_PROBE)) {
            rateLimiter(limit = ServerConfigs.configs.rateLimiterProbeLimit, refillPeriod = ServerConfigs.configs.rateLimiterProbeRefillPeriod.milliseconds)
            requestKey { call -> call.getRequestKey(trustedProxyHops = trustedProxyHops) }
        }
        register(name = RateLimitName(name = RATE_LIMIT_BROADCAST)) {
            rateLimiter(limit = ServerConfigs.configs.rateLimiterBroadcastLimit, refillPeriod = ServerConfigs.configs.rateLimiterBroadcastRefillPeriod.milliseconds)
            requestKey { call -> call.getRequestKey(trustedProxyHops = trustedProxyHops) }
        }
    }
}

/**
 * Retrieves the key a rate limiter buckets this call under, which is the caller's address.
 *
 * @receiver [ApplicationCall] The context of the call.
 * @param trustedProxyHops How many reverse proxies sit in front of this server.
 * @return The address to bucket the caller under.
 */
private fun ApplicationCall.getRequestKey(trustedProxyHops: Int): String = resolveRequestKey(
    forwardedFor = request.headers[HttpHeaders.XForwardedFor],
    trustedProxyHops = trustedProxyHops,
    remoteAddress = request.origin.remoteAddress
)

/**
 * Resolves the caller's address from the socket address and the forwarding chain.
 *
 * @param forwardedFor The raw `X-Forwarded-For` header, if any.
 * @param trustedProxyHops How many reverse proxies sit in front of this server. Zero disables the header entirely.
 * @param remoteAddress The address the request was received from, used whenever the chain cannot be trusted or is shorter than the hop count.
 * @return The address to bucket the caller under.
 */
internal fun resolveRequestKey(forwardedFor: String?, trustedProxyHops: Int, remoteAddress: String): String {
    if (trustedProxyHops <= 0) return remoteAddress
    val forwarded = forwardedFor
        ?.split(',')
        ?.map { it.trim() }
        ?.filter { it.isNotEmpty() }
        .orEmpty()
    return forwarded.getOrNull(index = forwarded.size - trustedProxyHops) ?: remoteAddress
}

/*** Name of the probe rate-limiting. */
const val RATE_LIMIT_PROBE = "probe"
/*** Name of the broadcast rate-limiting. */
const val RATE_LIMIT_BROADCAST = "broadcast"
