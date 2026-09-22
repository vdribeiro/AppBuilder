package com.app.builder.core.flow

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Class designed for lazy initialization within a coroutine-based environment.
 * It ensures that [get] is only performed once and the result is cached for all subsequent calls.
 */
class LazyData<T>(private val load: suspend () -> T) {
    /** Guards [_data] so [load] runs at most once even under concurrent calls to [get]. */
    private val mutex = Mutex()
    /** The cached result of [load], once computed. */
    private var _data: T? = null
    /**
     * Returns the cached value, loading it via [load] on first access.
     * Concurrent callers are serialized on [mutex], so [load] runs at most once.
     *
     * @return The value produced by [load], cached for every subsequent call.
     */
    suspend fun get(): T = _data ?: mutex.withLock { _data ?: load().also { _data = it } }
}