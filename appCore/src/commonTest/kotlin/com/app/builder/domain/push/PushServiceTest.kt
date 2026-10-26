package com.app.builder.domain.push

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.uuid.Uuid
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import io.ktor.client.engine.mock.toByteArray
import io.ktor.http.HttpStatusCode
import com.app.builder.core.config.ClientFlags
import com.app.builder.data.device.getDeviceUuid
import com.app.builder.data.http.HttpClientFactory
import com.app.builder.data.http.Query
import com.app.builder.data.http.TestEngine
import com.app.builder.data.http.URL
import com.app.builder.data.http.notFound
import com.app.builder.data.http.respondMock
import com.app.builder.data.http.respondWebSocket
import com.app.builder.data.serializer.decode
import com.app.builder.data.serializer.encode
import com.app.builder.domain.DeviceToken
import com.app.builder.domain.User
import com.app.builder.test.FakeData
import com.app.builder.test.TestCase

class PushServiceTest: TestCase() {

    /** The ticket fixture handed out by the mock server. */
    private val ticketUuid = Uuid.random()

    /** Number of ticket requests received by the mock server. */
    private val ticketRequests = MutableStateFlow(value = 0)

    /** Number of push stream connections received by the mock server. */
    private val pushConnections = MutableStateFlow(value = 0)

    /** The ticket uuid query parameter of every push stream connection, in order. */
    private val connectedTickets = mutableListOf<String?>()

    /** The device uuid query parameter of every push stream connection, in order. */
    private val connectedDevices = mutableListOf<String?>()

    /** The device token bodies received by the mock server, in order. */
    private val registeredTokens = MutableStateFlow(value = emptyList<DeviceToken>())

    /**
     * Builds a [PushService] backed by a mock server that hands out [ticketUuid] and streams [FakeData.notification] on every push connection.
     *
     * @param user The user the service listens for.
     * @param ticketStatus The status the ticket endpoint answers with.
     * @return The fixture [PushService] instance.
     */
    private fun pushService(user: User, ticketStatus: HttpStatusCode = HttpStatusCode.OK): PushService = PushManager(
        user = user,
        httpClient = HttpClientFactory(engine = TestEngine { request ->
            when (request.url.encodedPath) {
                URL.Tickets.path -> {
                    ticketRequests.update { it + 1 }
                    respondMock(status = ticketStatus, content = encode(value = ticketUuid))
                }

                URL.Push.path -> {
                    connectedTickets.add(element = request.url.parameters[Query.TicketUuid.parameter])
                    connectedDevices.add(element = request.url.parameters[Query.DeviceUuid.parameter])
                    pushConnections.update { it + 1 }
                    respondWebSocket(pushPayload = FakeData.notification)
                }

                URL.DeviceTokens.path -> {
                    val token = decode<DeviceToken>(value = request.body.toByteArray().decodeToString())
                    if (token != null) registeredTokens.update { it + token }
                    respondMock()
                }

                else -> notFound(request = request)
            }
        }).httpClient
    )

    /** Verifies that a payload addressed to the current user is handled and delivered through [PushProvider.pushPayloads]. */
    @Test
    fun payloadForCurrentUserIsDelivered() = runUnitTest {
        val pushService = pushService(user = FakeData.adminUser)

        pushService.start()
        val delivered = PushProvider.pushPayloads.first()
        pushService.stop()

        assertEquals(expected = FakeData.notification, actual = delivered)
    }

    /** Verifies that the push stream is opened with the requested ticket and this device's uuid. */
    @Test
    fun pushStreamUsesTheRequestedTicketAndDeviceUuid() = runUnitTest {
        val pushService = pushService(user = FakeData.adminUser)

        pushService.start()
        pushConnections.first { it > 0 }
        pushService.stop()

        assertEquals(expected = ticketUuid.toString(), actual = connectedTickets.first())
        assertEquals(expected = getDeviceUuid().toString(), actual = connectedDevices.first())
    }

    /** Verifies that a payload addressed to a different user is dropped instead of being handled. */
    @Test
    fun payloadForDifferentUserIsDropped() = runUnitTest {
        val pushService = pushService(user = FakeData.user)

        pushService.start()
        // A second ticket request is only made once the first stream, including its payload, has been fully processed
        ticketRequests.first { it > 1 }
        pushService.stop()

        assertTrue(actual = PushProvider.handlePushPayload(pushPayload = FakeData.notification), message = "the payload was already handled")
    }

    /** Verifies that a failed ticket request is retried without opening the push stream. */
    @Test
    fun failedTicketRequestIsRetriedWithoutConnecting() = runUnitTest {
        val pushService = pushService(user = FakeData.adminUser, ticketStatus = HttpStatusCode.InternalServerError)

        pushService.start()
        ticketRequests.first { it > 1 }
        pushService.stop()

        assertEquals(expected = 0, actual = pushConnections.value)
    }

    /** Verifies that a push token set on [PushProvider] is registered with the server alongside this device's uuid. */
    @Test
    fun pushTokenIsRegisteredWithTheDeviceUuid() = runUnitTest {
        val pushService = pushService(user = FakeData.adminUser)

        pushService.start()
        PushProvider.setToken(token = "token")
        val registered = registeredTokens.first { it.isNotEmpty() }
        pushService.stop()

        assertEquals(expected = listOf(DeviceToken(deviceUuid = getDeviceUuid(), token = "token")), actual = registered)
    }

    /** Verifies that starting the service does nothing while the push feature flag is disabled. */
    @Test
    fun startIsIgnoredWhilePushIsDisabled() = runUnitTest {
        ClientFlags.set { it.copy(push = false) }
        val pushService = pushService(user = FakeData.adminUser)

        pushService.start()
        PushProvider.setToken(token = "token")
        testScheduler.runCurrent()
        pushService.stop()

        assertEquals(expected = 0, actual = ticketRequests.value)
        assertTrue(actual = registeredTokens.value.isEmpty())
        assertTrue(actual = PushProvider.handlePushPayload(pushPayload = FakeData.notification), message = "the payload was already handled")
    }
}
