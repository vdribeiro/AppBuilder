package com.app.builder.domain.usecase.notification

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.flow.firstOrNull
import com.app.builder.test.FakeData
import com.app.builder.test.TestCase

class NotificationsUseCasesTest: TestCase() {

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
}
