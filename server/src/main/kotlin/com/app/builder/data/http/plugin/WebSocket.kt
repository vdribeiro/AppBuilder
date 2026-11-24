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
        pingPeriod = ServerConfigs.value.pushPingInterval.milliseconds
        timeout = ServerConfigs.value.pushTimeout.milliseconds
        maxFrameSize = MAX_FRAME_BYTES
        masking = false
    }
}

/** The largest incoming frame accepted, in bytes. A bigger frame closes the connection. */
private const val MAX_FRAME_BYTES = 64L * 1024
