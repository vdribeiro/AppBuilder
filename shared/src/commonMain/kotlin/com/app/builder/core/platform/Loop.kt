package com.app.builder.core.platform

import kotlinx.coroutines.delay

/**
 * A function that loops indefinitely with a delay.
 * Traditionally, an infinite loop is a code smell that pegs the CPU at 100% and drains the battery in minutes.
 * Fortunately, `delay` does not block the thread. It suspends the coroutine.
 * Even so, it is not a "zero cost" operation, even if it is computationally trivial, lest we abuse it with a tiny delay interval.
 *
 * @param timeMillis The delay interval in milliseconds.
 * @param block The suspending block to be executed in each loop iteration.
 */
suspend fun loop(timeMillis: Long = 3000L, block: suspend () -> Unit) {
    while (true) {
        block()
        delay(timeMillis = timeMillis)
    }
}
