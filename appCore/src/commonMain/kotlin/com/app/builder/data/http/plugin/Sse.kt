package com.app.builder.data.http.plugin

import io.ktor.client.HttpClientConfig
import io.ktor.client.engine.HttpClientEngineConfig
import io.ktor.client.plugins.sse.SSE

/**
 * Installs and configures the [SSE] plugin for server-sent events.
 *
 * @param T The underlying HTTP client engine configuration type.
 */
internal fun <T: HttpClientEngineConfig> HttpClientConfig<T>.installSse() {
    install(plugin = SSE)
}
