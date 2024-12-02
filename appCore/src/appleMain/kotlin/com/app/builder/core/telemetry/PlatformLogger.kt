package com.app.builder.core.telemetry

import platform.Foundation.NSLog

actual object PlatformLogger: TelemetryEngine {

    actual override fun info(tag: String, message: String) {
        NSLog(format = "INFO [$tag]: $message")
    }

    actual override fun error(tag: String, message: String, throwable: Throwable?) {
        NSLog(format = "ERROR [$tag]: $message${throwable?.let { "\n${it.stackTraceToString()}" }.orEmpty()}")
    }

    actual override fun feedback(message: String) {
        NSLog(format = "FEEDBACK: $message")
    }
}
