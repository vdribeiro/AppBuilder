package com.app.builder.core.telemetry

actual object PlatformLogger: TelemetryEngine {

    actual override fun info(tag: String, message: String) {
        println("INFO [$tag]: $message")
    }

    actual override fun error(tag: String, message: String, throwable: Throwable?) {
        println("ERROR [$tag]: $message${throwable?.let { "\n${it.stackTraceToString()}" }.orEmpty()}")
    }

    actual override fun feedback(message: String) {
        println("FEEDBACK: $message")
    }
}
