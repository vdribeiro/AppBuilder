package com.app.builder.core.telemetry

import org.slf4j.LoggerFactory

/** Server logger. */
object ServerLogger: TelemetryEngine {

    /** Logger instance. */
    private val logger = LoggerFactory.getLogger("ServerTelemetry")

    override fun info(tag: String, message: String) {
        logger.info("[$tag]: $message")
    }

    override fun error(tag: String, message: String, throwable: Throwable?) {
        logger.error("[$tag]: $message", throwable)
    }

    override fun feedback(message: String) {
        logger.info("[FEEDBACK]: $message")
    }
}
