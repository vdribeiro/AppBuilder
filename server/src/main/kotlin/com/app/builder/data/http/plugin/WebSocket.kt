package com.app.builder.data.http.plugin

import kotlin.time.Duration.Companion.milliseconds
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.websocket.WebSockets
import io.ktor.server.websocket.pingPeriod
import io.ktor.server.websocket.timeout
import com.app.builder.core.config.ServerConfigs

/** Installs and configures web sockets. */
fun Application.installWebSocket() {
    install(plugin = WebSockets) {
        pingPeriod = ServerConfigs.configs.pushPingInterval.milliseconds
        timeout = ServerConfigs.configs.pushTimeout.milliseconds
        maxFrameSize = Long.MAX_VALUE
        masking = false
    }
}
