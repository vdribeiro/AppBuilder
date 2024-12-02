package com.app.builder.core.telemetry

/** A mock implementation of the [TelemetryEngine] interface. */
class MockLogger: TelemetryEngine {

    override fun info(tag: String, message: String) {
        println("INFO: $tag - $message")
    }

    override fun error(tag: String, message: String, throwable: Throwable?) {
        println("ERROR: $tag - $message${if (throwable == null) "" else "\n${throwable.message}"}")
    }

    override fun feedback(message: String) {
        println("FEEDBACK: $message")
    }
}