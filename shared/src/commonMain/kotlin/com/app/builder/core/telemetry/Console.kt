package com.app.builder.core.telemetry

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import com.app.builder.core.config.ClientFlags
import com.app.builder.core.telemetry.Console.CONSOLE_SIZE
import com.app.builder.core.telemetry.Console.dispatcher

/**
 * A thread-safe in-memory log buffer.
 *
 * This object maintains a circular-style buffer of the most recent [CONSOLE_SIZE] log entries.
 * It uses a single-threaded dispatcher to synchronize updates to the retained lines, ensuring consistency across concurrent coroutine calls without platform-specific locks.
 */
object Console: TelemetryEngine {
    /** The maximum number of log lines to retain in memory. */
    private const val CONSOLE_SIZE = 1000

    /** A specialized dispatcher limited to a single thread to act as a synchronization lock. */
    private val dispatcher = Dispatchers.Default.limitedParallelism(parallelism = 1)
    /** Internal scope for asynchronous logging operations, using a [SupervisorJob] to ensure that a failure in one log operation doesn't cancel the console entirely. */
    private val scope = CoroutineScope(context = dispatcher + SupervisorJob())

    /** Backing state for [logs]. Updates are restricted to the [dispatcher] for safety. */
    private val _logs: MutableStateFlow<List<String>> = MutableStateFlow(value = emptyList())
    /** The retained log lines, ordered from oldest to newest. */
    val logs: StateFlow<List<String>> = _logs.asStateFlow()

    init {
        scope.launch {
            ClientFlags.flow
                .map { it.console }
                .distinctUntilChanged()
                .filter { !it }
                .collect { _logs.value = emptyList() }
        }
    }

    /**
     * Asynchronously adds a log message to the buffer.
     *
     * The message is split by line breaks, and each non-empty line is appended. If the buffer exceeds [CONSOLE_SIZE], the oldest entries are discarded.
     *
     * @param log The raw log string to be added.
     */
    private fun log(log: String) {
        if (!ClientFlags.value.console) return
        scope.launch {
            if (!ClientFlags.value.console) return@launch
            val lines = log.split("\n").filter { it.isNotEmpty() }
            _logs.value = (_logs.value + lines).takeLast(n = CONSOLE_SIZE)
        }
    }

    override fun info(tag: String, message: String) {
        log(log = "INFO [$tag]: $message")
    }

    override fun error(tag: String, message: String, throwable: Throwable?) {
        log(log = "ERROR [$tag]: $message${throwable?.let { "\n${it.stackTraceToString()}" }.orEmpty()}")
    }

    override fun feedback(message: String) {
        log(log = "FEEDBACK: $message")
    }
}
