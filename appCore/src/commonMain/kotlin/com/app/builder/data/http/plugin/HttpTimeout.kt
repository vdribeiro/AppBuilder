package com.app.builder.data.http.plugin

import io.ktor.client.HttpClientConfig
import io.ktor.client.engine.HttpClientEngineConfig
import io.ktor.client.plugins.HttpTimeout
import com.app.builder.core.config.ClientConfigs

/**
 * Configures the [HttpTimeout] plugin to prevent indefinite hanging on poor connections.
 * Enforces strict limits for connection, socket, and overall request durations.
 *
 * @param T The underlying HTTP client engine configuration type.
 * @param connectTimeoutMillis The maximum time in milliseconds to establish a connection with the server.
 * @param socketTimeoutMillis The maximum time in milliseconds to read or write data from or to the server.
 * @param requestTimeoutMillis The maximum time in milliseconds to wait for a complete response from the server.
 */
internal fun <T: HttpClientEngineConfig> HttpClientConfig<T>.installHttpTimeout(
    connectTimeoutMillis: Long = ClientConfigs.configs.httpConnectTimeoutMillis,
    socketTimeoutMillis: Long = ClientConfigs.configs.httpSocketTimeoutMillis,
    requestTimeoutMillis: Long = ClientConfigs.configs.httpRequestTimeoutMillis,
) {
    install(plugin = HttpTimeout) {
        this.connectTimeoutMillis = connectTimeoutMillis
        this.socketTimeoutMillis = socketTimeoutMillis
        this.requestTimeoutMillis = requestTimeoutMillis
    }
}

