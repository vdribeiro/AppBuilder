package com.app.builder.core.telemetry

/** Interface for capturing diagnostic telemetry data. */
interface TelemetryEngine {

    /**
     * Logs an informational event.
     *
     * @param tag The identifier used to categorize the log source.
     * @param message The detailed log message.
     */
    fun info(tag: String, message: String)

    /**
     * Logs a diagnostic error or exception.
     *
     * @param tag The identifier for the error source.
     * @param message The description of the failure.
     * @param throwable The optional exception payload containing stack trace and context details.
     */
    fun error(tag: String, message: String, throwable: Throwable? = null)

    /**
     * Processes user-submitted feedback.
     *
     * @param message The textual content provided by the user.
     */
    fun feedback(message: String)
}
