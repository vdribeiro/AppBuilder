package com.app.builder.core.telemetry

import kotlin.test.Test
import kotlin.test.assertEquals
import com.app.builder.test.TestCase

class TelemetryTest: TestCase() {

    /** A [TelemetryEngine] that records every call it receives. */
    private class RecordingEngine: TelemetryEngine {
        /** The calls received, in order. */
        val calls = mutableListOf<String>()

        override fun info(tag: String, message: String) {
            calls += "info:$tag:$message"
        }

        override fun error(tag: String, message: String, throwable: Throwable?) {
            calls += "error:$tag:$message:${throwable?.message}"
        }

        override fun feedback(message: String) {
            calls += "feedback:$message"
        }
    }

    /** Verifies info, error and feedback are dispatched to every installed engine with their arguments. */
    @Test
    fun dispatchesToEveryEngine() = runUnitTest {
        val engines = listOf(RecordingEngine(), RecordingEngine())
        Telemetry.engines = engines

        Telemetry.info(tag = "TAG", message = "info message")
        Telemetry.error(tag = "TAG", message = "error message", throwable = IllegalStateException("boom"))
        Telemetry.feedback(message = "feedback message")

        engines.forEach { engine ->
            assertEquals(
                expected = listOf("info:TAG:info message", "error:TAG:error message:boom", "feedback:feedback message"),
                actual = engine.calls
            )
        }
    }

    /** Verifies error forwards a null throwable when none is given. */
    @Test
    fun errorDefaultsToNullThrowable() = runUnitTest {
        val engine = RecordingEngine()
        Telemetry.engines = listOf(engine)

        Telemetry.error(tag = "TAG", message = "error message")

        assertEquals(expected = listOf("error:TAG:error message:null"), actual = engine.calls)
    }

    /** Verifies an engine's error defaults to a null throwable when called without one. */
    @Test
    fun engineErrorDefaultsToNullThrowable() = runUnitTest {
        val engine = RecordingEngine()
        engine.error(tag = "TAG", message = "error message")

        assertEquals(expected = listOf("error:TAG:error message:null"), actual = engine.calls)
    }

    /** Verifies dispatching with no installed engines is a no-op. */
    @Test
    fun noEnginesIsNoOp() = runUnitTest {
        Telemetry.engines = emptyList()

        Telemetry.info(tag = "TAG", message = "info message")
        Telemetry.error(tag = "TAG", message = "error message")
        Telemetry.feedback(message = "feedback message")

        assertEquals(expected = emptyList(), actual = Telemetry.engines)
    }
}
