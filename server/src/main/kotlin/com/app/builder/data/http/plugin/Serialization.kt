package com.app.builder.data.http.plugin

import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation

/**
 * Installs and configures the [ContentNegotiation] plugin for the Ktor [Application].
 * This enables the server to negotiate media types for incoming requests and outgoing responses, specifically configuring it to automatically serialize and deserialize JSON content.
 */
fun Application.installSerialization() {
    install(plugin = ContentNegotiation) {
        json()
    }
}