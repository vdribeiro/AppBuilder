package com.app.builder.data.signal

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map

/**
 * An in-process [InstanceSignal], reaching only observers within this instance.
 * Suitable for single-instance deployments and tests. Multi-instance deployments require shared transport.
 */
class LocalSignal: InstanceSignal {

    /** Emits channel to payload pairs. */
    private val signals = MutableSharedFlow<Pair<String, String>>(extraBufferCapacity = 64)

    /** The number of active observers across all channels. Signals published while there are no observers are dropped. */
    val subscriptions: StateFlow<Int> = signals.subscriptionCount

    override suspend fun notify(channel: String, payload: String): Boolean = signals.tryEmit(value = channel to payload)

    override fun observe(channel: String): Flow<String> = signals
        .filter { (signalChannel, _) -> signalChannel == channel }
        .map { (_, payload) -> payload }
}
