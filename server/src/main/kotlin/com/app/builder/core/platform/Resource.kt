package com.app.builder.core.platform

import kotlinx.coroutines.withContext
import com.app.builder.core.flow.Dispatcher
import com.app.builder.core.flow.runSuspendCatching
import com.app.builder.core.telemetry.Telemetry

/**
 * Reads a text file from the server's classpath resources.
 *
 * @param path The path of the file relative to `main/resources`.
 * @return The text of the file, or `null` if it doesn't exist or can't be read.
 */
suspend fun readResource(path: String): String? = withContext(context = Dispatcher.IO) {
    runSuspendCatching {
        Thread.currentThread().contextClassLoader.getResource(path)?.readText()
    }.onFailure {
        Telemetry.error(tag = TAG, message = "Unable to read resource $path", throwable = it)
    }.getOrNull()
}

private const val TAG = "Resource"
