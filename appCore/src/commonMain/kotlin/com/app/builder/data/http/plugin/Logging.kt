package com.app.builder.data.http.plugin

import io.ktor.client.HttpClientConfig
import io.ktor.client.engine.HttpClientEngineConfig
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import com.app.builder.core.telemetry.Telemetry

/**
 * Configures the Ktor [Logging] plugin to intercept and route network logs through the [Telemetry] system.
 *
 * @param T The underlying HTTP client engine configuration type.
 * @param logLevel The level of detail to be logged.
 */
internal fun <T: HttpClientEngineConfig> HttpClientConfig<T>.installLogging(logLevel: LogLevel = LogLevel.ALL) {
    install(plugin = Logging) {
        logger = object: Logger {
            override fun log(message: String) {
                Telemetry.info(tag = TAG, message = message)
            }
        }
        level = logLevel
    }
}

private const val TAG = "HttpLogging"
