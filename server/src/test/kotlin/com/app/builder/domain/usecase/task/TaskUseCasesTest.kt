package com.app.builder.domain.usecase.task

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.days
import kotlinx.coroutines.flow.firstOrNull
import com.app.builder.core.config.ServerConfigs
import com.app.builder.core.locale.now
import com.app.builder.test.FakeData
import com.app.builder.test.TestCase

class TaskUseCasesTest: TestCase() {

    /** Verifies that upserting a task makes it retrievable and triggers a notification for its users. */
    @Test
    fun crudTasks() = runServerTest {
        val taskUseCases = dependency.get()
            .useCases
            .taskUseCases
        val notificationsUseCases = dependency.get()
            .useCases
            .notificationsUseCases
        val lastSyncUtc = now() - 1.days

        assertTrue(actual = notificationsUseCases.observeNotifications(userUuid = FakeData.adminUser.uuid).firstOrNull().orEmpty().isEmpty())
        assertTrue(actual = taskUseCases.getTasks(userUuid = FakeData.adminUser.uuid, lastSyncUtc = lastSyncUtc, cursorUtc = null, cursorUuid = null, pageSize = ServerConfigs.configs.pageSize).isEmpty())
        assertTrue(actual = taskUseCases.upsertTask(userUuid = FakeData.adminUser.uuid, task = FakeData.task))
        assertEquals(expected = listOf(element = FakeData.task), actual = taskUseCases.getTasks(userUuid = FakeData.adminUser.uuid, lastSyncUtc = lastSyncUtc, cursorUtc = null, cursorUuid = null, pageSize = ServerConfigs.configs.pageSize))
        assertTrue(actual = notificationsUseCases.observeNotifications(userUuid = FakeData.adminUser.uuid).firstOrNull().orEmpty().isNotEmpty())
    }

    /** Verifies that a non-owning user cannot update an existing task. */
    @Test
    fun upsertTaskRejectsNonOwner() = runServerTest {
        val taskUseCases = dependency.get()
            .useCases
            .taskUseCases

        assertTrue(actual = taskUseCases.upsertTask(userUuid = FakeData.adminUser.uuid, task = FakeData.task))
        assertFalse(actual = taskUseCases.upsertTask(userUuid = FakeData.user.uuid, task = FakeData.task.copy(title = "Hijacked")))
    }
}
