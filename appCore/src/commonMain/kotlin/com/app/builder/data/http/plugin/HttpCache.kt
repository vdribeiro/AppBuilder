package com.app.builder.data.http.plugin

import io.ktor.client.HttpClientConfig
import io.ktor.client.engine.HttpClientEngineConfig
import io.ktor.client.plugins.cache.HttpCache
import io.ktor.client.plugins.cache.storage.CacheStorage

/**
 * Configures the [HttpCache] plugin with unlimited storage for both public and private caches.
 *
 * @param T The underlying HTTP client engine configuration type.
 */
internal fun <T: HttpClientEngineConfig> HttpClientConfig<T>.installHttpCache() {
    install(plugin = HttpCache) {
        publicStorage(storage = CacheStorage.Unlimited())
        privateStorage(storage = CacheStorage.Unlimited())
        isShared = false
    }
}