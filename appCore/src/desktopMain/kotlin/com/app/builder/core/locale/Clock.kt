package com.app.builder.core.locale

import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import com.app.builder.core.platform.loop

actual fun observeClockChanges(): Flow<Unit> = flow {
    var lastSystemNow = now()
    var lastMonotonicMark = markNow()

    loop {
        val systemNow = now()
        val systemElapsed = systemNow - lastSystemNow
        val monotonicElapsed = lastMonotonicMark.elapsedNow()
        val divergence = (systemElapsed - monotonicElapsed).absoluteValue

        lastSystemNow = systemNow
        lastMonotonicMark = markNow()

        if (divergence > CLOCK_CHANGE_THRESHOLD) emit(value = Unit)
    }
}

/** Minimum divergence between elapsed system and monotonic time to be treated as a clock change. */
private val CLOCK_CHANGE_THRESHOLD: Duration = 2.seconds
