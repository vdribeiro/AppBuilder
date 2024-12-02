package com.app.builder.data.http

import io.ktor.client.engine.HttpClientEngine
import com.app.builder.test.ExcludeFromTesting

/**
 * Creates a platform-specific [HttpClientEngine] instance.
 *
 * @return An [HttpClientEngine] configured for the current target platform.
 */
@ExcludeFromTesting
expect fun createHttpEngine(): HttpClientEngine
