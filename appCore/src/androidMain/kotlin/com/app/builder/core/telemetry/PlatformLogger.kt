package com.app.builder.core.telemetry

import android.util.Log
import com.app.builder.core.telemetry.PlatformLogger.CHUNK_SIZE

actual object PlatformLogger: TelemetryEngine {

    actual override fun info(tag: String, message: String) {
        log(message = message) { Log.i(tag, it) }
    }

    actual override fun error(tag: String, message: String, throwable: Throwable?) {
        log(message = "$message${throwable?.let { "\n${it.stackTraceToString()}" }.orEmpty()}") { Log.e(tag, it) }
    }

    actual override fun feedback(message: String) {
        log(message = message) { Log.i("FEEDBACK", it) }
    }

    /**
     * Helper to handle Logcat's 4KB buffer limit.
     *
     * If the [message] exceeds [CHUNK_SIZE], it is split into multiple parts and logged sequentially with an index prefix.
     *
     * @param message The raw string to be logged.
     * @param log A lambda representing the specific [Log] level function to call.
     */
    private fun log(message: String, log: (String) -> Unit) {
        if (message.length <= CHUNK_SIZE) log(message)
        else message.chunked(CHUNK_SIZE).forEachIndexed { index, chunk -> log("($index) $chunk") }
    }

    /**
     * The maximum number of characters per log entry.
     * Based on Logcat's ~4KB limit, assuming 2 bytes per character to provide a safe buffer.
     */
    private const val CHUNK_SIZE = 2000
}
