package com.app.builder.data.http.plugin

import kotlin.test.Test
import kotlin.test.assertTrue
import io.ktor.server.application.plugin
import io.ktor.server.websocket.WebSockets
import com.app.builder.test.TestCase

class WebSocketTest: TestCase() {

    /** Verifies that the largest incoming frame accepted is at most 1 MiB. */
    @Test
    fun frameSizeIsBounded() = runServerTest {
        var maxFrameSize = Long.MAX_VALUE
        application {
            installWebSocket()
            maxFrameSize = plugin(plugin = WebSockets).maxFrameSize
        }
        startApplication()
        assertTrue(actual = maxFrameSize <= 1024 * 1024, message = "Max frame size: $maxFrameSize")
    }
}
