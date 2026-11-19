package com.app.builder.domain.usecase.notification

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.flow.firstOrNull
import com.app.builder.core.config.ServerFlags
import com.app.builder.test.FakeData
import com.app.builder.test.TestCase

class NotificationsUseCasesTest: TestCase() {

    override suspend fun beforeTest() {
        super.beforeTest()
        ServerFlags.set { it.copy(push = true) }
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

    /** Verifies that a notification is not saved while the push flag is off. */
    @Test
    fun saveNotificationIsNoOpWhenPushIsDisabled() = runServerTest {
        ServerFlags.set { it.copy(push = false) }
        val notificationsUseCases = dependency.get()
            .useCases
            .notificationsUseCases

        assertFalse(actual = notificationsUseCases.saveNotification(notification = FakeData.entityNotification))
        assertTrue(actual = notificationsUseCases.observeNotifications(userUuid = FakeData.adminUser.uuid).firstOrNull().orEmpty().isEmpty())
    }
}
