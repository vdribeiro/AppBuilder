package com.app.builder.domain.push

import kotlinx.coroutines.delay

/**
 * Attempts [send] up to [maxAttempts] times, doubling the wait between failed attempts starting from [retryDelay].
 *
 * @param maxAttempts The maximum number of attempts.
 * @param retryDelay The initial wait between attempts, in milliseconds.
 * @param send The delivery attempt to execute.
 * @return `true` as soon as an attempt succeeds, `false` once every attempt has failed.
 */
suspend fun sendWithRetry(maxAttempts: Int, retryDelay: Long, send: suspend () -> Unit): Boolean {
    var delayMillis = retryDelay
    repeat(times = maxAttempts) { attempt ->
        runCatching {
            send()
            return true
        }.onFailure {
            if (attempt < maxAttempts - 1) {
                delay(timeMillis = delayMillis)
                delayMillis *= 2
            }
        }
    }
    return false
}
