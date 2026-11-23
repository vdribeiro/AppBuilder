package com.app.builder.domain.push

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.minutes
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import io.ktor.client.engine.mock.toByteArray
import com.app.builder.core.config.ClientFlags
import com.app.builder.core.flow.Dispatcher
import com.app.builder.data.http.HttpClientFactory
import com.app.builder.data.http.TestEngine
import com.app.builder.data.http.URL
import com.app.builder.data.http.notFound
import com.app.builder.data.http.respondMock
import com.app.builder.data.http.respondSse
import com.app.builder.data.serializer.decode
import com.app.builder.domain.DeviceToken
import com.app.builder.domain.PushPayload
import com.app.builder.test.TestCase

class BroadcastServiceTest: TestCase() {

    override suspend fun beforeTest() {
        super.beforeTest()
        ClientFlags.set { it.copy(http = true, broadcastStream = true, broadcastFirebase = true) }
    }

    /** The broadcast payload fixture, addressed to no particular user. */
    private val broadcast = PushPayload.Flags()

    /** Number of broadcast stream connections received by the mock server. */
    private val broadcastConnections = MutableStateFlow(value = 0)

    /** The device token bodies received by the mock server's subscribe endpoint, in order. */
    private val subscribedTokens = MutableStateFlow(value = emptyList<DeviceToken>())

    /**
     * Builds a [BroadcastService] backed by a mock server that streams [broadcast] on every broadcast connection.
     *
     * @return The fixture [BroadcastService] instance.
     */
    private fun broadcastService(): BroadcastService = BroadcastManager(
        httpClient = HttpClientFactory(engine = TestEngine { request ->
            when (request.url.encodedPath) {
                URL.Broadcast.path -> {
                    broadcastConnections.update { it + 1 }
                    respondSse(pushPayload = broadcast)
                }

                URL.BroadcastSubscribe.path -> {
                    val token = decode<DeviceToken>(value = request.body.toByteArray().decodeToString())
                    if (token != null) subscribedTokens.update { it + token }
                    respondMock()
                }

                else -> notFound(request = request)
            }
        }).httpClient
    )

    /** Verifies that a broadcast event is handled and delivered through [PushProvider.pushPayloads]. */
    @Test
    fun broadcastIsDelivered() = runUnitTest {
        val broadcastService = broadcastService()

        broadcastService.start()
        val delivered = PushProvider.pushPayloads.first()
        broadcastService.stop()

        assertEquals(expected = broadcast, actual = delivered)
    }

    /** Verifies that the stream reconnects once the server closes it, delivering the redelivered duplicate only once. */
    @Test
    fun streamReconnectsAfterItCloses() = runUnitTest {
        val received = mutableListOf<PushPayload>()
        backgroundScope.launch(context = Dispatcher.Default) { PushProvider.pushPayloads.collect { received.add(element = it) } }
        val broadcastService = broadcastService()

        broadcastService.start()
        broadcastConnections.first { it > 2 }
        broadcastService.stop()
        testScheduler.runCurrent()

        assertEquals(expected = listOf<PushPayload>(broadcast), actual = received)
    }

    /** Verifies that a push token set on [PushProvider] is subscribed to the broadcast topic. */
    @Test
    fun pushTokenIsSubscribedToBroadcasts() = runUnitTest {
        val broadcastService = broadcastService()

        broadcastService.start()
        PushProvider.setToken(token = "token")
        val subscribed = subscribedTokens.first { it.isNotEmpty() }
        broadcastService.stop()

        assertEquals(expected = listOf(DeviceToken(token = "token")), actual = subscribed)
    }

    /** Verifies that starting an already running service is a no-op, so the push token is subscribed only once. */
    @Test
    fun startingTwiceKeepsASingleSubscription() = runUnitTest {
        val broadcastService = broadcastService()

        broadcastService.start()
        broadcastService.start()
        PushProvider.setToken(token = "token")
        subscribedTokens.first { it.isNotEmpty() }
        testScheduler.runCurrent()
        broadcastService.stop()

        assertEquals(expected = 1, actual = subscribedTokens.value.size)
    }

    /** Verifies that a stopped service can be started again, subscribing the push token anew. */
    @Test
    fun restartingAfterStopSubscribesTheTokenAgain() = runUnitTest {
        val broadcastService = broadcastService()
        PushProvider.setToken(token = "token")
        broadcastService.start()
        subscribedTokens.first { it.size == 1 }
        broadcastService.stop()

        broadcastService.start()
        subscribedTokens.first { it.size == 2 }
        broadcastService.stop()
    }

    /** Verifies that a running service connects when the broadcast stream and broadcast Firebase feature flags turn on and stops reconnecting when they turn off. */
    @Test
    fun runningServiceFollowsTheBroadcastFlag() = runUnitTest {
        ClientFlags.set { it.copy(broadcastStream = false, broadcastFirebase = false) }
        val broadcastService = broadcastService()
        broadcastService.start()
        testScheduler.runCurrent()
        assertEquals(expected = 0, actual = broadcastConnections.value)

        ClientFlags.set { it.copy(broadcastStream = true, broadcastFirebase = true) }
        broadcastConnections.first { it > 0 }

        ClientFlags.set { it.copy(broadcastStream = false, broadcastFirebase = false) }
        testScheduler.runCurrent()
        val connections = broadcastConnections.value
        testScheduler.advanceTimeBy(delayTime = 10.minutes)
        broadcastService.stop()

        assertEquals(expected = connections, actual = broadcastConnections.value)
    }

    /** Verifies that, while the broadcast stream feature flag is off, a running service subscribes the push token without opening the stream, and connects once the flag turns on. */
    @Test
    fun runningServiceFollowsTheBroadcastStreamFlag() = runUnitTest {
        ClientFlags.set { it.copy(broadcastStream = false) }
        val broadcastService = broadcastService()
        broadcastService.start()
        PushProvider.setToken(token = "token")
        subscribedTokens.first { it.isNotEmpty() }
        testScheduler.runCurrent()
        assertEquals(expected = 0, actual = broadcastConnections.value)

        ClientFlags.set { it.copy(broadcastStream = true) }
        broadcastConnections.first { it > 0 }

        ClientFlags.set { it.copy(broadcastStream = false) }
        testScheduler.runCurrent()
        val connections = broadcastConnections.value
        testScheduler.advanceTimeBy(delayTime = 10.minutes)
        broadcastService.stop()

        assertEquals(expected = connections, actual = broadcastConnections.value)
    }

    /** Verifies that, while the broadcast Firebase feature flag is off, a running service opens the stream without subscribing the push token, and subscribes it once the flag turns on. */
    @Test
    fun runningServiceFollowsTheBroadcastFirebaseFlag() = runUnitTest {
        ClientFlags.set { it.copy(broadcastFirebase = false) }
        val broadcastService = broadcastService()
        broadcastService.start()
        PushProvider.setToken(token = "token")
        broadcastConnections.first { it > 0 }
        testScheduler.runCurrent()
        assertTrue(actual = subscribedTokens.value.isEmpty())

        ClientFlags.set { it.copy(broadcastFirebase = true) }
        subscribedTokens.first { it.isNotEmpty() }
        broadcastService.stop()
    }

    /** Verifies that starting the service does nothing while the broadcast stream and broadcast Firebase feature flags are disabled. */
    @Test
    fun startIsIgnoredWhileBroadcastIsDisabled() = runUnitTest {
        ClientFlags.set { it.copy(broadcastStream = false, broadcastFirebase = false) }
        val broadcastService = broadcastService()

        broadcastService.start()
        PushProvider.setToken(token = "token")
        testScheduler.runCurrent()
        broadcastService.stop()

        assertEquals(expected = 0, actual = broadcastConnections.value)
        assertTrue(actual = subscribedTokens.value.isEmpty())
        assertTrue(actual = PushProvider.handlePushPayload(pushPayload = broadcast), message = "the broadcast was already handled")
    }
}
