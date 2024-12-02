package com.app.builder.data.http.plugin

import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.plugins.calllogging.CallLogging
import io.ktor.server.request.path
import org.slf4j.event.Level

/**
 * Installs and configures the [CallLogging] plugin for the Ktor [Application].
 * This logs incoming HTTP client requests to the server at the [Level.INFO] level.
 * The logging is filtered to only include requests where the URL path starts with "/".
 */
fun Application.installLogging() {
    install(plugin = CallLogging) {
        level = Level.INFO
        filter { call -> call.request.path().startsWith(prefix = "/") }
    }
}