package com.app.builder.core.flow

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.updateAndGet

/**
 * Holds a value that can be read synchronously, observed as a stream and mutated atomically, starting from and restorable to a [default].
 *
 * @property default The initial value, restored by [reset].
 */
open class StateHolder<T>(private val default: T) {
    /** Backing state flow tracking value changes. */
    private val _flow: MutableStateFlow<T> = MutableStateFlow(value = default)
    /** Read only stream, emitting the current value on collection and every change after. */
    val flow: StateFlow<T> = _flow.asStateFlow()
    /** Current synchronous snapshot of the value. */
    val value: T get() = _flow.value

    /**
     * Atomically mutates the value and returns the freshly evaluated one.
     *
     * @param function A lambda block receiving the current snapshot and returning a modified copy.
     * @return The updated value.
     */
    fun set(function: (T) -> T): T = _flow.updateAndGet(function = function)

    /**
     * Restores the default value.
     *
     * @return The default value.
     */
    fun reset(): T = set { default }
}
