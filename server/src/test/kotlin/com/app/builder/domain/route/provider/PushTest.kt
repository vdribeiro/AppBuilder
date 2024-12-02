package com.app.builder.domain.route.provider

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlin.uuid.Uuid
import io.ktor.client.call.body
import io.ktor.http.HttpStatusCode
import io.ktor.websocket.CloseReason
import io.ktor.websocket.Frame
import io.ktor.websocket.close
import com.app.builder.core.security.uuid
import com.app.builder.data.http.HttpRequest
import com.app.builder.data.http.Query
import com.app.builder.data.http.URL
import com.app.builder.data.http.post
import com.app.builder.data.http.webSocket
import com.app.builder.domain.DeviceToken
import com.app.builder.domain.PushPayload
import com.app.builder.test.FakeData
import com.app.builder.test.TestCase

class PushTest: TestCase() {

    /** Verifies that generating a push ticket requires authentication and returns a ticket UUID. */
    @Test
    fun generateTicket() = runServerTest {
        installRouting()

        val client = createClient(token = null)
        assertEquals(expected = HttpStatusCode.Unauthorized, actual = client.post<Unit>(request = HttpRequest(url = URL.Tickets)).status)

        val authenticatedClient = createClient(token = bearerToken.accessToken)
        val response = authenticatedClient.post<Unit>(request = HttpRequest(url = URL.Tickets))
        assertEquals(expected = HttpStatusCode.OK, actual = response.status)
        assertNotNull(actual = response.body<Uuid>())
    }

    /** Verifies that registering a device token succeeds for an authenticated client. */
    @Test
    fun deviceToken() = runServerTest {
        installRouting()

        val client = createClient(token = bearerToken.accessToken)
        val deviceUuid = uuid()
        val registration = DeviceToken(deviceUuid = deviceUuid, token = "fcmToken")
        val response = client.post<DeviceToken>(request = HttpRequest(url = URL.DeviceTokens), body = registration)
        assertEquals(expected = HttpStatusCode.OK, actual = response.status)
    }

    /** Verifies that registering a device token requires authentication. */
    @Test
    fun deviceTokenUnauthenticated() = runServerTest {
        installRouting()

        val client = createClient(token = null)
        val registration = DeviceToken(deviceUuid = uuid(), token = "fcmToken")
        assertEquals(expected = HttpStatusCode.Unauthorized, actual = client.post<DeviceToken>(request = HttpRequest(url = URL.DeviceTokens), body = registration).status)
    }

    /** Verifies that registering a device token rejects a blank token or a missing device uuid. */
    @Test
    fun deviceTokenInvalidParameters() = runServerTest {
        installRouting()

        val client = createClient(token = bearerToken.accessToken)
        assertEquals(
            expected = HttpStatusCode.BadRequest,
            actual = client.post<DeviceToken>(request = HttpRequest(url = URL.DeviceTokens), body = DeviceToken(deviceUuid = uuid(), token = "")).status
        )
        assertEquals(
            expected = HttpStatusCode.BadRequest,
            actual = client.post<DeviceToken>(request = HttpRequest(url = URL.DeviceTokens), body = DeviceToken(deviceUuid = null, token = "fcmToken")).status
        )
    }

    /** Verifies that sending a push notification requires notification write permission and succeeds for an authorized user. */
    @Test
    fun sendPushNotification() = runServerTest {
        installRouting()

        val client = createClient(token = null)
        assertEquals(expected = HttpStatusCode.Unauthorized, actual = client.post(request = HttpRequest(url = URL.Push), body = FakeData.notification as PushPayload).status)

        // FakeData.user has no permission at all for the notification entity type.
        val userClient = createClient(token = bearerToken.accessToken)
        assertEquals(expected = HttpStatusCode.Forbidden, actual = userClient.post(request = HttpRequest(url = URL.Push), body = FakeData.notification as PushPayload).status)

        val adminClient = createClient(token = adminBearerToken.accessToken)
        assertEquals(expected = HttpStatusCode.OK, actual = adminClient.post(request = HttpRequest(url = URL.Push), body = FakeData.notification as PushPayload).status)
    }

    /** Verifies that the push WebSocket rejects invalid or missing tickets and connects with a valid one. */
    @Test
    fun push() = runServerTest {
        installRouting()

        val client = createClient(token = null)
        client.webSocket<Frame.Text>(request = HttpRequest(url = URL.Push), onConnected = { assertEquals(expected = CloseReason.Codes.VIOLATED_POLICY, actual = closeReason.await()?.knownReason) }) {}

        val deviceUuid = uuid()
        client.webSocket<Frame.Text>(
            request = HttpRequest(
                url = URL.Push,
                queryMap = mapOf(Query.TicketUuid to uuid().toString(), Query.DeviceUuid to deviceUuid.toString())
            ),
            onConnected = { assertEquals(expected = CloseReason.Codes.VIOLATED_POLICY, actual = closeReason.await()?.knownReason) }
        ) {}

        val authenticatedClient = createClient(token = bearerToken.accessToken)
        val ticketUuid = authenticatedClient.post<Unit>(request = HttpRequest(url = URL.Tickets)).body<Uuid>()

        var connectionEstablished = false
        client.webSocket<Frame.Text>(
            request = HttpRequest(
                url = URL.Push,
                queryMap = mapOf(Query.TicketUuid to ticketUuid.toString(), Query.DeviceUuid to deviceUuid.toString())
            ),
            onConnected = {
                connectionEstablished = true
                send(frame = Frame.Text(text = uuid().toString()))
                close()
            }
        ) {}
        assertTrue(actual = connectionEstablished)
    }
}
