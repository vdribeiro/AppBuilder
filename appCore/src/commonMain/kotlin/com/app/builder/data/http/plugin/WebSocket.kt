package com.app.builder.data.http.plugin

import kotlin.time.Duration.Companion.milliseconds
import kotlinx.serialization.json.Json
import io.ktor.client.HttpClientConfig
import io.ktor.client.engine.HttpClientEngineConfig
import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.client.plugins.websocket.pingInterval
import io.ktor.serialization.kotlinx.KotlinxWebsocketSerializationConverter
import com.app.builder.core.config.ClientConfigs

/**
 * Configures the [WebSockets] plugin.
 *
 * @param T The underlying HTTP client engine configuration type.
 * @param pingInterval The interval in milliseconds between ping frames sent to the server.
 */
internal fun <T: HttpClientEngineConfig> HttpClientConfig<T>.installWebSocket(
    pingInterval: Long = ClientConfigs.configs.pushPingInterval
) {
    install(plugin = WebSockets) {
        contentConverter = KotlinxWebsocketSerializationConverter(format = Json)
        this.pingInterval = pingInterval.milliseconds
    }
}
