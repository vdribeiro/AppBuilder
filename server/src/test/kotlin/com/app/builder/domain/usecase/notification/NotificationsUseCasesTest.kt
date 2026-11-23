package com.app.builder.domain.usecase.notification

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withTimeout
import com.app.builder.core.config.ServerFlags
import com.app.builder.test.FakeData
import com.app.builder.test.TestCase

class NotificationsUseCasesTest: TestCase() {

    override suspend fun beforeTest() {
        super.beforeTest()
        ServerFlags.set { it.copy(pushSocket = true) }
    }

    /** Verifies that a notification can be saved, observed, and deleted. */
    @Test
    fun crudNotification() = runServerTest {
        val notificationsUseCases = dependency.get()
            .useCases
            .notificationsUseCases

        val userUuid = FakeData.adminUser.uuid
        assertNull(actual = notificationsUseCases.observeNotifications(userUuid = userUuid).firstOrNull()?.firstOrNull { it == FakeData.entityNotification })
        assertTrue(actual = notificationsUseCases.saveNotification(notification = FakeData.entityNotification))
        assertEquals(expected = FakeData.entityNotification, actual = notificationsUseCases.observeNotifications(userUuid = userUuid).firstOrNull()?.firstOrNull { it == FakeData.entityNotification })
        assertTrue(actual = notificationsUseCases.deleteNotification(notificationUuid = FakeData.entityNotification.uuid))
        assertNull(actual = notificationsUseCases.observeNotifications(userUuid = userUuid).firstOrNull()?.firstOrNull { it == FakeData.entityNotification })
    }

    /** Verifies that a saved notification is emitted to the saved notifications observer. */
    @Test
    fun savedNotificationIsEmitted() = runServerTest {
        val notificationsUseCases = dependency.get()
            .useCases
            .notificationsUseCases

        coroutineScope {
            val saved = async(start = CoroutineStart.UNDISPATCHED) { notificationsUseCases.observeSavedNotifications().first() }
            assertTrue(actual = notificationsUseCases.saveNotification(notification = FakeData.entityNotification))
            assertEquals(expected = FakeData.entityNotification, actual = withTimeout(timeMillis = 5_000L) { saved.await() })
        }
    }

    /** Verifies that a notification is not saved while the push socket and push Firebase flags are off. */
    @Test
    fun saveNotificationIsNoOpWhenPushIsDisabled() = runServerTest {
        ServerFlags.set { it.copy(pushSocket = false) }
        val notificationsUseCases = dependency.get()
            .useCases
            .notificationsUseCases

        assertFalse(actual = notificationsUseCases.saveNotification(notification = FakeData.entityNotification))
        assertTrue(actual = notificationsUseCases.observeNotifications(userUuid = FakeData.adminUser.uuid).firstOrNull().orEmpty().isEmpty())
    }
}
