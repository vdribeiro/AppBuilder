package com.app.builder.data.http.plugin

import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.sse.SSE

/** Installs and configures the [SSE] plugin for server-sent events. */
fun Application.installSse() {
    install(plugin = SSE)
}
