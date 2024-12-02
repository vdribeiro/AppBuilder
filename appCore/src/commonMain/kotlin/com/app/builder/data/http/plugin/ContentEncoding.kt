package com.app.builder.data.http.plugin

import io.ktor.client.HttpClientConfig
import io.ktor.client.engine.HttpClientEngineConfig
import io.ktor.client.plugins.compression.ContentEncoding

/**
 * Configures the [ContentEncoding] plugin to support Gzip compressed HTTP responses.
 *
 * @param T The underlying HTTP client engine configuration type.
 */
internal fun <T: HttpClientEngineConfig> HttpClientConfig<T>.installContentEncoding() {
    install(plugin = ContentEncoding) {
        gzip()
    }
}