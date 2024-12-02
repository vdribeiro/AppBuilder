package com.app.builder.core.config

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.updateAndGet
import kotlinx.serialization.Serializable

/** Centralized config container to change system capabilities dynamically. */
@Serializable
data class ClientConfigs(
    /** Maximum width that determines if a screen is split. */
    val splitScreenMaximumWidth: Int,
    /** Default fraction of split-screen width given to the end pane versus the start pane from 1 to 99 percent. */
    val splitPaneRatio: Int,
    /** Maximum time in milliseconds allowed to establish a connection with the server. */
    val httpConnectTimeoutMillis: Long,
    /** Maximum time in milliseconds of inactivity allowed between incoming data packets. */
    val httpSocketTimeoutMillis: Long,
    /** Maximum total time in milliseconds allowed to process an HTTP call from request to response. */
    val httpRequestTimeoutMillis: Long,
    /**
     * Maximum milliseconds the device clock may drift from the server clock before it is flagged as untrustworthy.
     * Keep in a sane range relative to the server's [ServerConfigs.timestampFutureTolerance]. If the client trusts a drift the server won't tolerate, its requests start failing timestamp validation.
     */
    val clockDriftTolerance: Long,
    /** Delay in milliseconds between clock recalibration probes while the client's clock is untrusted. The first probe fires immediately on distrust. The loop stops once trust is regained. */
    val clockProbeInterval: Long,
    /** Maximum number of attempts before a job is marked permanently as failed instead of retried. */
    val schedulerMaxAttempts: Int,
    /** The maximum number of push payloads to remember. */
    val maxHandledPayloads: Int,
    /** Initial delay in milliseconds before the first broadcast reconnect attempt, and the value the backoff resets to after a successful reconnect. Doubles on each subsequent failure up to [broadcastMaxRetryDelay]. */
    val broadcastRetryDelay: Long,
    /** Ceiling in milliseconds for the broadcast reconnect backoff described in [broadcastRetryDelay]. */
    val broadcastMaxRetryDelay: Long,
    /** Interval in milliseconds at which the client's WebSocket client sends keep-alive PING frames. Applies to the push connection and any other WebSocket opened through the shared HTTP client. */
    val pushPingInterval: Long,
    /** Initial delay in milliseconds before the first push WebSocket reconnect attempt, reset on a successful reconnect and doubling on each failure up to [pushMaxRetryDelay]. */
    val pushRetryDelay: Long,
    /** Ceiling in milliseconds for the push reconnect backoff described in [pushRetryDelay]. */
    val pushMaxRetryDelay: Long,
    /** Maximum number of parameters chunked into a single lookup. Must stay under SQLite's bound-parameter limit per statement. */
    val batchSize: Int,
    /** Minimum displacement in meters the device must move before a new location fix is delivered. */
    val locationDisplacementMeters: Float,
    /** Fastest rate in milliseconds at which location fixes may be delivered by the steady cadence. Updates are never more frequent than this value. */
    val locationIntervalMillis: Long,
    /**
     * Fastest rate in milliseconds at which location fixes may be delivered during the burst that opens every capture start.
     * It raises accuracy and drops batching and displacement filtering, so it costs far more power than [locationIntervalMillis] and is only meant to run until the first fresh fix lands or [locationBurstTimeoutMillis] elapses.
     */
    val locationBurstIntervalMillis: Long,
    /** Maximum time in milliseconds the burst described in [locationBurstIntervalMillis] may run before the loop settles back to [locationIntervalMillis], even when no fresh fix landed. */
    val locationBurstTimeoutMillis: Long,
) {
    companion object {

        /** Default configs. */
        private val default = ClientConfigs(
            splitScreenMaximumWidth = 600,
            splitPaneRatio = 60,
            httpConnectTimeoutMillis = 20_000L,
            httpSocketTimeoutMillis = 50_000L,
            httpRequestTimeoutMillis = 120_000L,
            clockDriftTolerance = 60_000L,
            clockProbeInterval = 30_000L,
            schedulerMaxAttempts = 3,
            maxHandledPayloads = 500,
            broadcastRetryDelay = 2_000L,
            broadcastMaxRetryDelay = 30_000L,
            pushPingInterval = 30_000L,
            pushRetryDelay = 2_000L,
            pushMaxRetryDelay = 30_000L,
            batchSize = 900,
            locationDisplacementMeters = 10f,
            locationIntervalMillis = 1_800_000L,
            locationBurstIntervalMillis = 5_000L,
            locationBurstTimeoutMillis = 60_000L,
        )

        /** Backing state flow tracking config changes. */
        private val _configsFlow: MutableStateFlow<ClientConfigs> = MutableStateFlow(value = default)
        /** Current synchronous snapshot of the configs. */
        val configs: ClientConfigs get() = _configsFlow.value

        /**
         * Atomically mutates the feature configuration state block and returns the freshly evaluated state.
         *
         * @param configs A lambda block receiving the current [ClientConfigs] snapshot and returning a modified copy.
         * @return The updated [ClientConfigs] instance.
         */
        fun set(configs: (ClientConfigs) -> ClientConfigs): ClientConfigs = _configsFlow.updateAndGet(function = configs)

        /**
         * Restores the configs to their default values.
         *
         * @return The default [ClientConfigs] instance.
         */
        fun reset(): ClientConfigs = set { default }
    }
}
