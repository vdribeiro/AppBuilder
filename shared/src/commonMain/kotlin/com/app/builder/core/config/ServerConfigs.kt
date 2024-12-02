package com.app.builder.core.config

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.updateAndGet
import kotlinx.serialization.Serializable

/** Centralized config container to change system capabilities dynamically. */
@Serializable
data class ServerConfigs(
    /** Maximum number of milliseconds a client timestamp may be ahead of server arrival time before the request is rejected. Widening this reduces tamper-resistance. Interacts with the client's [ClientConfigs.clockDriftTolerance]. */
    val timestampFutureTolerance: Long,
    /** Access token validity in milliseconds. Independent of [refreshTokenValidity]. A shorter value just forces more frequent silent refreshes. */
    val accessTokenValidity: Long,
    /** Refresh token validity in milliseconds. Should be meaningfully larger than [accessTokenValidity], since it's the true session lifetime the access token is refreshed within. */
    val refreshTokenValidity: Long,
    /** Global per-IP rate limiter maximum requests per period, applied to every route not covered by a specific limiter. */
    val rateLimiterLimit: Int,
    /** Global rate limiter refill period in milliseconds. */
    val rateLimiterRefillPeriod: Long,
    /** Per-IP rate limiter maximum requests per period applied to the health check route. */
    val rateLimiterProbeLimit: Int,
    /** Probe rate limiter refill period in milliseconds. */
    val rateLimiterProbeRefillPeriod: Long,
    /** Per-IP rate limiter maximum requests per period applied to the broadcast routes. */
    val rateLimiterBroadcastLimit: Int,
    /** Broadcast rate limiter refill period in milliseconds. */
    val rateLimiterBroadcastRefillPeriod: Long,
    /** Interval in milliseconds at which the server sends SSE heartbeat pings on the broadcast route, to keep the connection alive and detect dead ones. */
    val broadcastPingInterval: Long,
    /** Maximum number of concurrent SSE broadcast connections accepted by this server instance. Enforced per-instance in memory, not cluster-wide. */
    val broadcastMaxConnections: Int,
    /**
     * Maximum number of attempts to deliver a broadcast payload to a session before giving up and dropping the connection.
     * The retry delay [broadcastRetryDelay] doubles after every failed attempt with no ceiling, so a high value combined with a non-trivial delay can make one delivery take a long time.
     */
    val broadcastMaxAttempts: Int,
    /** Initial delay in milliseconds before retrying a failed broadcast payload delivery. Doubles on each subsequent attempt up to [broadcastMaxAttempts], with no maximum-delay cap. */
    val broadcastRetryDelay: Long,
    /** Interval in milliseconds at which the server sends WebSocket PING frames to push clients. Paired with [pushTimeout] as the liveness check for push connections. */
    val pushPingInterval: Long,
    /** Time in milliseconds the server waits for a push WebSocket to respond before considering it dead. Should leave room for at least one ping/pong round trip relative to [pushPingInterval]. */
    val pushTimeout: Long,
    /** Maximum number of concurrent push WebSocket connections across the whole cluster, based on live rows in the presence table rather than just this instance. */
    val pushMaxConnections: Int,
    /** Maximum number of concurrent push WebSocket connections allowed for a single user, enforced within the global [pushMaxConnections] cap. It is effectively the binding limit for a single user's multi-device fan-out. */
    val pushMaxUserConnections: Int,
    /**
     * Maximum number of attempts to deliver a push payload to a device before giving up and dropping the connection. The retry delay [pushRetryDelay] doubles after every failed attempt with no ceiling,
     * so a high value combined with a non-trivial delay can make one delivery take a long time.
     */
    val pushMaxAttempts: Int,
    /** Initial delay in milliseconds before retrying a failed push payload delivery. It doubles on each subsequent attempt up to [pushMaxAttempts], with no maximum-delay cap. */
    val pushRetryDelay: Long,
    /** Time in milliseconds a one-time connection ticket remains valid after issuance. An expired ticket is deleted and rejected on use, so the client must request a fresh one before opening the push WebSocket. */
    val ticketTtl: Long,
    /**
     * Interval in milliseconds at which this server instance refreshes the heartbeat of its live connection presence rows. Must stay comfortably below [presenceTtl],
     * or a delayed heartbeat can cause a still-connected device to be wrongly pruned as dead.
     */
    val presenceHeartbeatInterval: Long,
    /** Age in milliseconds beyond which a connection presence row without a heartbeat is considered dead and eligible for pruning. Should be meaningfully larger than [presenceHeartbeatInterval] (see there). */
    val presenceTtl: Long,
    /** Default number of entries returned per page when the client omits the page size. Clamped through the same [maxPageSize] bound as any client-supplied value. */
    val pageSize: Int,
    /** Upper bound on entries returned per page. */
    val maxPageSize: Int,
) {
    companion object {

        /** Default configs. */
        private val default = ServerConfigs(
            timestampFutureTolerance = 60_000L,
            accessTokenValidity = 3_600_000L,
            refreshTokenValidity = 2_592_000_000L,
            rateLimiterLimit = 200,
            rateLimiterRefillPeriod = 60_000L,
            rateLimiterProbeLimit = 5,
            rateLimiterProbeRefillPeriod = 30_000L,
            rateLimiterBroadcastLimit = 10,
            rateLimiterBroadcastRefillPeriod = 60_000L,
            broadcastPingInterval = 15_000L,
            broadcastMaxConnections = 10_000,
            broadcastMaxAttempts = 3,
            broadcastRetryDelay = 2_000L,
            pushPingInterval = 15_000L,
            pushTimeout = 15_000L,
            pushMaxConnections = 10_000,
            pushMaxUserConnections = 10,
            pushMaxAttempts = 3,
            pushRetryDelay = 2_000L,
            ticketTtl = 30_000L,
            presenceHeartbeatInterval = 15_000L,
            presenceTtl = 45_000L,
            pageSize = 200,
            maxPageSize = 100_000,
        )

        /** Backing state flow tracking config changes. */
        private val _configsFlow: MutableStateFlow<ServerConfigs> = MutableStateFlow(value = default)
        /** Current synchronous snapshot of the configs. */
        val configs: ServerConfigs get() = _configsFlow.value

        /**
         * Atomically mutates the config configuration state block and returns the freshly evaluated state.
         *
         * @param configs A lambda block receiving the current [ServerConfigs] snapshot and returning a modified copy.
         * @return The updated [ServerConfigs] instance.
         */
        fun set(configs: (ServerConfigs) -> ServerConfigs): ServerConfigs = _configsFlow.updateAndGet(function = configs)

        /**
         * Restores the configs to their default values.
         *
         * @return The default [ServerConfigs] instance.
         */
        fun reset(): ServerConfigs = set { default }
    }
}
