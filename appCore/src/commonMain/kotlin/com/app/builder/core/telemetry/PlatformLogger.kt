package com.app.builder.core.telemetry

/** Platform-specific logging utility for native system console outputs. */
expect object PlatformLogger: TelemetryEngine {

    /**
     * Logs an informational message to the native system console.
     *
     * @param tag The identifier used to categorize the log source.
     * @param message The detailed log message.
     */
    override fun info(tag: String, message: String)

    /**
     * Logs an error message and optional exception trace to the native system console.
     *
     * @param tag The identifier for the error source.
     * @param message The description of the failure.
     * @param throwable The optional exception payload containing stack trace and context details.
     */
    override fun error(tag: String, message: String, throwable: Throwable?)

    /**
     * Logs a user-provided feedback message to the native system console.
     *
     * @param message The textual content provided by the user.
     */
    override fun feedback(message: String)
}
