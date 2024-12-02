package com.app.builder.data.http.plugin

import kotlin.test.Test
import kotlin.test.assertEquals
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngineConfig
import io.ktor.client.plugins.sse.serverSentEvents
import com.app.builder.data.http.TestEngine
import com.app.builder.data.http.baseUrl
import com.app.builder.data.http.respondSse
import com.app.builder.data.serializer.decode
import com.app.builder.domain.PushPayload
import com.app.builder.test.FakeData
import com.app.builder.test.TestCase

class SseTest: TestCase() {

    /** Verifies a server-sent event is received and decoded. */
    @Test
    fun receivesServerSentEvent() = runUnitTest {
        val client = HttpClient(engine = TestEngine(config = MockEngineConfig().apply {
            requestHandlers.add { respondSse(pushPayload = FakeData.notification) }
        })) {
            installSse()
        }

        var received: PushPayload? = null
        client.serverSentEvents(urlString = "$baseUrl/broadcast") {
            incoming.collect { event -> received = event.data?.let { decode<PushPayload>(value = it) } }
        }

        assertEquals(expected = FakeData.notification, actual = received)
    }
}
