package com.app.builder.domain.push

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.withTimeout
import com.app.builder.core.config.ServerConfigs
import com.app.builder.core.security.uuid
import com.app.builder.data.http.FakeWebSocketSession
import com.app.builder.data.signal.LocalSignal
import com.app.builder.domain.DeviceToken
import com.app.builder.domain.usecase.Gateways
import com.app.builder.test.FakeData
import com.app.builder.test.TestCase

class PushServiceTest: TestCase() {

    /** Verifies that a connection can be added with a valid ticket and removed afterward. */
    @Test
    fun addConnection() = runServerTest {
        val pushService = dependency.get().pushService
        val deviceUuid = uuid()
        val session = FakeWebSocketSession()

        val ticket = pushService.generateTicket(userUuid = FakeData.adminUser.uuid)
        assertNotNull(actual = ticket)

        assertEquals(expected = FakeData.adminUser.uuid, actual = pushService.addConnection(ticketUuid = ticket, deviceUuid = deviceUuid, session = session))
        assertNull(actual = pushService.addConnection(ticketUuid = ticket, deviceUuid = deviceUuid, session = FakeWebSocketSession()))
        assertNull(actual = pushService.addConnection(ticketUuid = uuid(), deviceUuid = deviceUuid, session = FakeWebSocketSession()))
        assertTrue(actual = pushService.removeConnection(userUuid = FakeData.adminUser.uuid, deviceUuid = deviceUuid, session = session))
    }

    /** Verifies that removing a stale connection does not evict a newer connection for the same device. */
    @Test
    fun staleRemovalDoesNotEvictReplacementConnection() = runServerTest {
        val useCases = dependency.get().useCases
        val pushService = dependency.get().pushService
        val deviceUuid = uuid()
        val deadSession = FakeWebSocketSession()
        val liveSession = FakeWebSocketSession()

        val ticket = pushService.generateTicket(userUuid = FakeData.adminUser.uuid)
        assertNotNull(actual = ticket)
        assertEquals(expected = FakeData.adminUser.uuid, actual = pushService.addConnection(ticketUuid = ticket, deviceUuid = deviceUuid, session = deadSession))

        val reconnectTicket = pushService.generateTicket(userUuid = FakeData.adminUser.uuid)
        assertNotNull(actual = reconnectTicket)
        assertEquals(expected = FakeData.adminUser.uuid, actual = pushService.addConnection(ticketUuid = reconnectTicket, deviceUuid = deviceUuid, session = liveSession))

        assertFalse(actual = pushService.removeConnection(userUuid = FakeData.adminUser.uuid, deviceUuid = deviceUuid, session = deadSession))
        assertEquals(expected = listOf(deviceUuid), actual = useCases.connectionUseCases.getConnectedDeviceUuids(userUuid = FakeData.adminUser.uuid))

        assertTrue(actual = pushService.removeConnection(userUuid = FakeData.adminUser.uuid, deviceUuid = deviceUuid, session = liveSession))
        assertTrue(actual = useCases.connectionUseCases.getConnectedDeviceUuids(userUuid = FakeData.adminUser.uuid).isEmpty())
    }

    /** Verifies that connections are rejected once the global or per-user connection limits are reached. */
    @Test
    fun addConnectionWithLimits() = runServerTest {
        ServerConfigs.set { it.copy(pushMaxConnections = 0) }
        val pushService = dependency.get().pushService
        val deviceUuid = uuid()

        val ticket = pushService.generateTicket(userUuid = FakeData.adminUser.uuid)
        assertNotNull(actual = ticket)

        assertNull(actual = pushService.addConnection(ticketUuid = ticket, deviceUuid = deviceUuid, session = FakeWebSocketSession()))

        ServerConfigs.set {
            it.copy(
                pushMaxConnections = 2,
                pushMaxUserConnections = 1
            )
        }

        val anotherTicket = pushService.generateTicket(userUuid = FakeData.adminUser.uuid)
        assertNotNull(actual = anotherTicket)
        assertEquals(expected = FakeData.adminUser.uuid, actual = pushService.addConnection(ticketUuid = anotherTicket, deviceUuid = deviceUuid, session = FakeWebSocketSession()))

        val yetAnotherTicket = pushService.generateTicket(userUuid = FakeData.adminUser.uuid)
        assertNotNull(actual = yetAnotherTicket)
        assertNull(actual = pushService.addConnection(ticketUuid = yetAnotherTicket, deviceUuid = uuid(), session = FakeWebSocketSession()))

        val reconnectTicket = pushService.generateTicket(userUuid = FakeData.adminUser.uuid)
        assertNotNull(actual = reconnectTicket)
        assertEquals(expected = FakeData.adminUser.uuid, actual = pushService.addConnection(ticketUuid = reconnectTicket, deviceUuid = deviceUuid, session = FakeWebSocketSession()))
    }

    /** Verifies that an expired ticket is rejected when adding a connection. */
    @Test
    fun expiredTicketIsRejected() = runServerTest {
        ServerConfigs.set { it.copy(ticketTtl = -1L) }
        val pushService = dependency.get().pushService

        val ticket = pushService.generateTicket(userUuid = FakeData.adminUser.uuid)
        assertNotNull(actual = ticket)
        assertNull(actual = pushService.addConnection(ticketUuid = ticket, deviceUuid = uuid(), session = FakeWebSocketSession()))
    }

    /** Verifies that a ticket generated on one instance can be redeemed to connect on another instance. */
    @Test
    fun ticketIsRedeemableAcrossInstances() = runServerTest {
        val pushService = dependency.get().pushService
        val otherInstance = createOtherInstance(fcmService = NoOpFcmService)
        val deviceUuid = uuid()

        val ticket = pushService.generateTicket(userUuid = FakeData.adminUser.uuid)
        assertNotNull(actual = ticket)

        assertEquals(expected = FakeData.adminUser.uuid, actual = otherInstance.addConnection(ticketUuid = ticket, deviceUuid = deviceUuid, session = FakeWebSocketSession()))
        assertNull(actual = pushService.addConnection(ticketUuid = ticket, deviceUuid = deviceUuid, session = FakeWebSocketSession()))
    }

    /** Verifies that FCM delivery skips devices connected via WebSocket on any instance and targets only offline ones. */
    @Test
    fun fcmSkipsDevicesConnectedOnAnyInstance() = runServerTest {
        val useCases = dependency.get().useCases
        val pushService = dependency.get().pushService
        val fakeFcmService = FakeFcmService()
        val otherInstance = createOtherInstance(fcmService = fakeFcmService)

        val connectedDeviceUuid = uuid()
        val offlineDeviceUuid = uuid()
        assertTrue(actual = useCases.deviceTokenUseCases.registerToken(userUuid = FakeData.adminUser.uuid, registration = DeviceToken(deviceUuid = connectedDeviceUuid, token = "connected-token")))
        assertTrue(actual = useCases.deviceTokenUseCases.registerToken(userUuid = FakeData.adminUser.uuid, registration = DeviceToken(deviceUuid = offlineDeviceUuid, token = "offline-token")))

        val connectedSession = FakeWebSocketSession()
        val ticket = pushService.generateTicket(userUuid = FakeData.adminUser.uuid)
        assertNotNull(actual = ticket)
        assertEquals(expected = FakeData.adminUser.uuid, actual = pushService.addConnection(ticketUuid = ticket, deviceUuid = connectedDeviceUuid, session = connectedSession))

        val notification = FakeData.notification

        otherInstance.sendNotification(pushPayload = notification)
        assertEquals(expected = listOf("offline-token"), actual = withTimeout(timeMillis = 5_000L) { fakeFcmService.multicasts.receive() })

        assertTrue(actual = pushService.removeConnection(userUuid = FakeData.adminUser.uuid, deviceUuid = connectedDeviceUuid, session = connectedSession))
        otherInstance.sendNotification(pushPayload = notification)
        assertEquals(expected = setOf("connected-token", "offline-token"), actual = withTimeout(timeMillis = 5_000L) { fakeFcmService.multicasts.receive() }.toSet())
    }

    /** Verifies that a notification saved on another instance is delivered over this instance's WebSocket connection. */
    @Test
    fun webSocketDeliversNotificationsSavedOnAnotherInstance() = runServerTest {
        val pushService = dependency.get().pushService
        val signal = dependency.get().instanceSignal as LocalSignal
        val session = FakeWebSocketSession()
        pushService.start()

        val ticket = pushService.generateTicket(userUuid = FakeData.adminUser.uuid)
        assertNotNull(actual = ticket)
        assertEquals(expected = FakeData.adminUser.uuid, actual = pushService.addConnection(ticketUuid = ticket, deviceUuid = uuid(), session = session))
        awaitUntil { signal.subscriptions.value > 0 }

        val otherGateways = Gateways(database = dependency.get().database, instanceSignal = signal)
        val notification = FakeData.entityNotification
        assertTrue(actual = otherGateways.notificationsUseCases.saveNotification(notification = notification))

        awaitUntil { session.sent }
        assertTrue(actual = session.sent)
        pushService.stop()
    }

    /** Verifies that acknowledging a push succeeds once and fails on a repeated acknowledgement. */
    @Test
    fun acknowledgePush() = runServerTest {
        val pushService = dependency.get().pushService
        val notificationsUseCases = dependency.get()
            .useCases
            .notificationsUseCases

        val notification = FakeData.entityNotification
        assertTrue(actual = notificationsUseCases.saveNotification(notification = notification))
        assertTrue(actual = pushService.acknowledgePush(pushUuid = notification.uuid))
        assertFalse(actual = pushService.acknowledgePush(pushUuid = notification.uuid))
    }

    /** Creates a second [PushManager] sharing this test's database, simulating another server instance. */
    private suspend fun createOtherInstance(fcmService: FcmService): PushManager {
        val useCases = dependency.get().useCases
        return PushManager(
            deviceTokenUseCases = useCases.deviceTokenUseCases,
            notificationsUseCases = useCases.notificationsUseCases,
            ticketUseCases = useCases.ticketUseCases,
            connectionUseCases = useCases.connectionUseCases,
            fcmService = fcmService
        )
    }
}
