package com.app.builder.core.telemetry

actual object PlatformLogger: TelemetryEngine {

    actual override fun info(tag: String, message: String) {
        val message = "INFO [$tag]: $message"
        info(message = message)
    }

    actual override fun error(tag: String, message: String, throwable: Throwable?) {
        val message = "ERROR [$tag]: $message${throwable?.let { "\n${it.stackTraceToString()}" }.orEmpty()}"
        error(message = message)
    }

    actual override fun feedback(message: String) {
        val message = "FEEDBACK: $message"
        info(message = message)
    }
}

/**
 * Logs [message] to the browser console at the info level.
 *
 * @param message The message to log.
 */
@JsFun(code = "(message) => console.info(message)")
private external fun info(message: String)

/**
 * Logs [message] to the browser console at the error level.
 *
 * @param message The message to log.
 */
@JsFun(code = "(message) => console.error(message)")
private external fun error(message: String)
