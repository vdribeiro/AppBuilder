package com.app.builder.data.http.plugin

import kotlin.test.Test
import kotlin.test.assertTrue
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngineConfig
import io.ktor.client.request.get
import com.app.builder.core.telemetry.Telemetry
import com.app.builder.core.telemetry.TelemetryEngine
import com.app.builder.data.http.TestEngine
import com.app.builder.data.http.baseUrl
import com.app.builder.data.http.respondMock
import com.app.builder.test.TestCase

class LoggingTest: TestCase() {

    /** Verifies request headers are logged through telemetry. */
    @Test
    fun logsRequestHeadersThroughTelemetry() = runUnitTest {
        val messages = mutableListOf<String>()
        Telemetry.engines.add(element = object: TelemetryEngine {
            override fun info(tag: String, message: String) {
                if (tag == "HttpLogging") messages.add(message)
            }

            override fun error(tag: String, message: String, throwable: Throwable?) {}
            override fun feedback(message: String) {}
        })

        val client = HttpClient(engine = TestEngine(config = MockEngineConfig().apply {
            requestHandlers.add { respondMock() }
        })) {
            installLogging()
        }

        client.get(urlString = baseUrl)

        assertTrue(actual = messages.isNotEmpty())
    }
}
