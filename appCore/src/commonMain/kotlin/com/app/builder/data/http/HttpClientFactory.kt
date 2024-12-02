package com.app.builder.data.http

import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.HttpClientEngineConfig
import io.ktor.client.plugins.auth.providers.BearerTokens
import io.ktor.client.plugins.logging.LogLevel
import com.app.builder.core.config.ClientConfigs
import com.app.builder.data.http.plugin.installAuth
import com.app.builder.data.http.plugin.installContentEncoding
import com.app.builder.data.http.plugin.installContentNegotiation
import com.app.builder.data.http.plugin.installHttpCache
import com.app.builder.data.http.plugin.installHttpTimeout
import com.app.builder.data.http.plugin.installInterceptor
import com.app.builder.data.http.plugin.installLogging
import com.app.builder.data.http.plugin.installSse
import com.app.builder.data.http.plugin.installWebSocket
import com.app.builder.core.platform.developmentMode as devMode

/**
 * Factory responsible for creating and configuring the [HttpClient].
 * Ensures that all network requests adhere to a consistent set of rules for logging, timeouts, caching, and serialization.
 *
 * @param engine The underlying networking engine used to execute HTTP requests.
 * @param configurations Optional configurations for the [HttpClient].
 */
class HttpClientFactory(engine: HttpClientEngine, private val configurations: Configurations = Configurations()) {

    /** The fully configured [HttpClient] instance ready for use. */
    val httpClient: HttpClient = HttpClient(engine = engine) { install() }

    /** Installs and configures the foundational plugins required for the [HttpClient] pipeline. */
    private fun <T: HttpClientEngineConfig> HttpClientConfig<T>.install() = with(receiver = configurations) {
        installLogging(logLevel = logLevel)
        installAuth(loadTokens = loadTokens, refreshTokens = refreshTokens)
        if (!devMode) installHttpTimeout(connectTimeoutMillis = connectTimeoutMillis, socketTimeoutMillis = socketTimeoutMillis, requestTimeoutMillis = requestTimeoutMillis)
        installHttpCache()
        installContentNegotiation()
        installContentEncoding()
        installInterceptor()
        installSse()
        installWebSocket(pingInterval = configurations.pingInterval)
    }
}

/**
 * Container for Http engine configurations.
 *
 * @property logLevel The level of detail to be logged.
 * @property loadTokens A suspendable function to retrieve the current [BearerTokens].
 * @property refreshTokens A suspendable function invoked upon a 401 Unauthorized response to obtain a refreshed set of [BearerTokens]. Returns null if the session cannot be renewed.
 * @property connectTimeoutMillis The maximum time in milliseconds to establish a connection with the server.
 * @property socketTimeoutMillis The maximum time in milliseconds to read or write data from or to the server.
 * @property requestTimeoutMillis The maximum time in milliseconds to wait for a complete response from the server.
 * @property pingInterval The interval in milliseconds between ping frames sent to the server.
 */
data class Configurations(
    val logLevel: LogLevel = LogLevel.HEADERS,
    val loadTokens: suspend () -> BearerTokens? = { null },
    val refreshTokens: suspend (BearerTokens?) -> BearerTokens? = { null },
    val connectTimeoutMillis: Long = ClientConfigs.configs.httpConnectTimeoutMillis,
    val socketTimeoutMillis: Long = ClientConfigs.configs.httpSocketTimeoutMillis,
    val requestTimeoutMillis: Long = ClientConfigs.configs.httpRequestTimeoutMillis,
    val pingInterval: Long = ClientConfigs.configs.pushPingInterval
)
