package com.app.builder.data.http.plugin

import kotlin.test.Test
import kotlin.test.assertTrue
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngineConfig
import io.ktor.websocket.Frame
import com.app.builder.data.http.HttpRequest
import com.app.builder.data.http.TestEngine
import com.app.builder.data.http.URL
import com.app.builder.data.http.respondWebSocket
import com.app.builder.data.http.webSocket
import com.app.builder.test.FakeData
import com.app.builder.test.TestCase

class WebSocketTest: TestCase() {

    /** Verifies the WebSockets plugin is installed and connects successfully. */
    @Test
    fun installsWebSocketsPlugin() = runUnitTest {
        val client = HttpClient(engine = TestEngine(config = MockEngineConfig().apply {
            requestHandlers.add { respondWebSocket(pushPayload = FakeData.notification) }
        })) {
            installWebSocket()
        }

        var connected = false
        client.webSocket<Frame.Text>(
            request = HttpRequest(url = URL.Push),
            onConnected = { connected = true },
            block = {}
        )

        assertTrue(actual = connected)
    }
}
