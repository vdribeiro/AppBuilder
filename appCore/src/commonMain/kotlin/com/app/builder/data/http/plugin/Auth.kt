package com.app.builder.data.http.plugin

import io.ktor.client.HttpClientConfig
import io.ktor.client.engine.HttpClientEngineConfig
import io.ktor.client.plugins.auth.Auth
import io.ktor.client.plugins.auth.providers.BearerTokens
import io.ktor.client.plugins.auth.providers.bearer

/**
 * Configures the Ktor [Auth] plugin with Bearer token authentication.
 *
 * @param T The underlying HTTP client engine configuration type.
 * @param loadTokens A suspendable function to retrieve the current [BearerTokens].
 * @param refreshTokens A suspendable function invoked upon a 401 Unauthorized response to obtain a refreshed set of [BearerTokens]. Returns null if the session cannot be renewed.
 */
internal fun <T: HttpClientEngineConfig> HttpClientConfig<T>.installAuth(
    loadTokens: suspend () -> BearerTokens? = { null },
    refreshTokens: suspend (BearerTokens?) -> BearerTokens? = { null }
) {
    install(plugin = Auth) {
        bearer {
            loadTokens { loadTokens() }
            refreshTokens { refreshTokens(oldTokens) }
        }
    }
}
