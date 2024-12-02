package com.app.builder.core.telemetry

/** Global hub for dispatching telemetry data, including logs, error reports, and user feedback. */
object Telemetry {

    /** The backing telemetry engines. */
    val engines: MutableList<TelemetryEngine> = mutableListOf()

    /**
     * Dispatches an informational log message.
     *
     * @param tag The component or feature area identifier.
     * @param message The detailed log message.
     */
    fun info(tag: String, message: String) {
        engines.forEach { it.info(tag = tag, message = message) }
    }

    /**
     * Dispatches an error log message with an optional exception payload.
     *
     * @param tag The component or feature area identifier.
     * @param message The error context description.
     * @param throwable The optional failure exception to log.
     */
    fun error(tag: String, message: String, throwable: Throwable? = null) {
        engines.forEach { it.error(tag = tag, message = message, throwable = throwable) }
    }

    /**
     * Dispatches a user-provided feedback report.
     *
     * @param message The raw textual content provided by the user.
     */
    fun feedback(message: String) {
        engines.forEach { it.feedback(message = message) }
    }
}
