package com.app.builder.core.flow

import kotlin.concurrent.Volatile
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

/** Centralized coroutine dispatchers. */
object Dispatcher {
    /** Dispatcher for UI-related operations. */
    @Volatile
    var Main: CoroutineDispatcher = Dispatchers.Main
    /** Dispatcher for CPU-intensive operations. */
    @Volatile
    var Default: CoroutineDispatcher = Dispatchers.Default
    /** Dispatcher for I/O-related operations. */
    @Volatile
    var IO: CoroutineDispatcher = io
}

/** Platform-specific I/O coroutine dispatcher. */
expect val io: CoroutineDispatcher
