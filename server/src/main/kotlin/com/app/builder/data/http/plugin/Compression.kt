package com.app.builder.data.http.plugin

import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.plugins.compression.Compression

/**
 * Installs and configures the [Compression] plugin for the Ktor [Application].
 * This enables the server to compress outbound content, reducing payload sizes and improving network performance for clients that support compressed responses.
 */
fun Application.installCompression() {
    install(plugin = Compression)
}