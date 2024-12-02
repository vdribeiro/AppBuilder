package com.app.builder.domain.route.provider

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import io.ktor.http.HttpStatusCode
import com.app.builder.data.http.HttpRequest
import com.app.builder.data.http.URL
import com.app.builder.data.http.post
import com.app.builder.data.http.sse
import com.app.builder.domain.DeviceToken
import com.app.builder.domain.PushPayload
import com.app.builder.test.TestCase

class BroadcastTest: TestCase() {

    /** Verifies that broadcasting requires authentication and a valid payload, and reaches connected SSE clients. */
    @Test
    fun broadcast() = runServerTest {
        installRouting()

        val client = createClient(token = null)
        assertEquals(expected = HttpStatusCode.Unauthorized, actual = client.post(request = HttpRequest(url = URL.Broadcast), body = PushPayload.Flags()).status)

        val authenticatedClient = createClient(token = adminBearerToken.accessToken)
        assertEquals(
            expected = HttpStatusCode.BadRequest,
            actual = authenticatedClient.post(
                request = HttpRequest(url = URL.Broadcast),
                body = "invalid"
            ).status
        )
        assertEquals(
            expected = HttpStatusCode.OK,
            actual = authenticatedClient.post(
                request = HttpRequest(url = URL.Broadcast),
                body = PushPayload.Flags() as PushPayload
            ).status
        )

        var sseEventReceived = false
        client.sse(
            request = HttpRequest(url = URL.Broadcast),
            onConnected = {
                launch {
                    authenticatedClient.post(
                        request = HttpRequest(url = URL.Broadcast),
                        body = PushPayload.Flags() as PushPayload
                    )
                }
            }
        ) {
            sseEventReceived = true
            cancel()
        }
        assertTrue(actual = sseEventReceived)
    }

    /** Verifies that subscribing a device token to the broadcast topic succeeds without authentication and rejects an invalid body. */
    @Test
    fun broadcastSubscribe() = runServerTest {
        installRouting()
        val client = createClient(token = null)

        assertEquals(expected = HttpStatusCode.BadRequest, actual = client.post(request = HttpRequest(url = URL.BroadcastSubscribe), body = "invalid body").status)

        val response = client.post(request = HttpRequest(url = URL.BroadcastSubscribe), body = DeviceToken(deviceUuid = null, token = "fcmToken"))
        assertEquals(expected = HttpStatusCode.OK, actual = response.status)
    }
}
