package com.app.builder.data.http.plugin

import kotlin.test.Test
import kotlin.test.assertFailsWith
import kotlinx.coroutines.delay
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngineConfig
import io.ktor.client.plugins.HttpRequestTimeoutException
import io.ktor.client.request.get
import com.app.builder.core.config.ClientConfigs
import com.app.builder.data.http.TestEngine
import com.app.builder.data.http.baseUrl
import com.app.builder.data.http.respondMock
import com.app.builder.test.TestCase

class HttpTimeoutTest: TestCase() {

    /** Verifies a timeout exception is thrown when the response is slower than the configured timeout. */
    @Test
    fun throwsOnSlowResponse() = runUnitTest {
        ClientConfigs.set { it.copy(httpRequestTimeoutMillis = 50L) }
        try {
            val client = HttpClient(engine = TestEngine(config = MockEngineConfig().apply {
                requestHandlers.add {
                    delay(timeMillis = 200L)
                    respondMock()
                }
            })) {
                installHttpTimeout()
            }

            assertFailsWith<HttpRequestTimeoutException> {
                client.get(urlString = "$baseUrl/slow")
            }
        } finally {
            ClientConfigs.reset()
        }
    }
}
