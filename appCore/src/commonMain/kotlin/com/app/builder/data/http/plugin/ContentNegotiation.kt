package com.app.builder.data.http.plugin

import io.ktor.client.HttpClientConfig
import io.ktor.client.engine.HttpClientEngineConfig
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.ContentType
import io.ktor.serialization.kotlinx.KotlinxSerializationConverter
import io.ktor.serialization.kotlinx.json.json
import com.app.builder.data.serializer.json

/**
 * Configures the [ContentNegotiation] plugin using Kotlinx Serialization.
 * Supports both standard JSON and fallback plain text responses to maximize API compatibility.
 *
 * @param T The underlying HTTP client engine configuration type.
 */
internal fun <T: HttpClientEngineConfig> HttpClientConfig<T>.installContentNegotiation() {
    install(plugin = ContentNegotiation) {
        json(json = json, contentType = ContentType.Application.Json)
        register(contentType = ContentType.Text.Plain, converter = KotlinxSerializationConverter(format = json))
    }
}