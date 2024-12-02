package com.app.builder.core.locale

import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlinx.browser.document
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import com.app.builder.core.platform.loop
import org.w3c.dom.events.Event

actual fun observeClockChanges(): Flow<Unit> = callbackFlow {
    var lastSystemNow = now()
    var lastMonotonicMark = markNow()

    fun checkDivergence() {
        val systemNow = now()
        val systemElapsed = systemNow - lastSystemNow
        val monotonicElapsed = lastMonotonicMark.elapsedNow()
        val divergence = (systemElapsed - monotonicElapsed).absoluteValue

        lastSystemNow = systemNow
        lastMonotonicMark = markNow()

        if (divergence > CLOCK_CHANGE_THRESHOLD) trySend(element = Unit)
    }

    val visibilityListener: (Event) -> Unit = { checkDivergence() }
    document.addEventListener(type = "visibilitychange", callback = visibilityListener)
    launch { loop { checkDivergence() } }
    awaitClose { document.removeEventListener(type = "visibilitychange", callback = visibilityListener) }
}

/** Minimum divergence between elapsed system and monotonic time to be treated as a clock change. */
private val CLOCK_CHANGE_THRESHOLD: Duration = 2.seconds
