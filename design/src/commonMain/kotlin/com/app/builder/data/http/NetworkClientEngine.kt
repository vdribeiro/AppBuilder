package com.app.builder.data.http

import io.ktor.client.engine.HttpClientEngine

/**
 * Creates a platform-specific [HttpClientEngine] instance.
 *
 * @return An [HttpClientEngine] configured for the current target platform.
 */
expect fun createNetworkEngine(): HttpClientEngine
