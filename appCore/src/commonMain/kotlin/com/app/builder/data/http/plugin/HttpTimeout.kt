package com.app.builder.data.http.plugin

import io.ktor.client.HttpClientConfig
import io.ktor.client.engine.HttpClientEngineConfig
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.api.createClientPlugin
import io.ktor.client.plugins.timeout
import com.app.builder.core.config.ClientConfigs

/**
 * Configures the [HttpTimeout] plugin to prevent indefinite hanging on poor connections.
 * Each request takes its connection, socket, and overall request limits from the [ClientConfigs] at the time it is sent.
 *
 * @param T The underlying HTTP client engine configuration type.
 */
internal fun <T: HttpClientEngineConfig> HttpClientConfig<T>.installHttpTimeout() {
    install(plugin = HttpTimeout)
    install(plugin = createClientPlugin(name = "HttpTimeoutConfigs") {
        onRequest { request, _ ->
            val configs = ClientConfigs.value
            request.timeout {
                connectTimeoutMillis = configs.httpConnectTimeoutMillis
                socketTimeoutMillis = configs.httpSocketTimeoutMillis
                requestTimeoutMillis = configs.httpRequestTimeoutMillis
            }
        }
    })
}
