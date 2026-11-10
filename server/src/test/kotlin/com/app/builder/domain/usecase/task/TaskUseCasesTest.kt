package com.app.builder.domain.usecase.task

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.days
import kotlin.time.Instant
import kotlinx.coroutines.flow.firstOrNull
import com.app.builder.core.config.ServerConfigs
import com.app.builder.core.locale.now
import com.app.builder.core.security.uuid
import com.app.builder.data.database.reset
import com.app.builder.domain.usecase.Outcome
import com.app.builder.test.FakeData
import com.app.builder.test.TestCase

class TaskUseCasesTest: TestCase() {

    /** Verifies that upserting a task makes it retrievable and triggers a notification for the users that can read tasks. */
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
        assertTrue(actual = taskUseCases.getTasks(lastSyncUtc = lastSyncUtc, cursorUtc = null, cursorUuid = null, pageSize = ServerConfigs.value.pageSize).successData().isEmpty())
        assertEquals(expected = Outcome.Success(data = Unit), actual = taskUseCases.upsertTask(task = FakeData.task))
        assertEquals(expected = listOf(element = FakeData.task), actual = taskUseCases.getTasks(lastSyncUtc = lastSyncUtc, cursorUtc = null, cursorUuid = null, pageSize = ServerConfigs.value.pageSize).successData())
        assertTrue(actual = notificationsUseCases.observeNotifications(userUuid = FakeData.adminUser.uuid).firstOrNull().orEmpty().isNotEmpty())
    }

    /** Verifies that fetching a single task returns it, and null for an unknown uuid. */
    @Test
    fun getTask() = runServerTest {
        val taskUseCases = dependency.get()
            .useCases
            .taskUseCases

        assertEquals(expected = Outcome.Success(data = Unit), actual = taskUseCases.upsertTask(task = FakeData.task))
        assertEquals(expected = FakeData.task, actual = taskUseCases.getTask(uuid = FakeData.task.uuid).successData())
        assertEquals(expected = Outcome.NotFound, actual = taskUseCases.getTask(uuid = uuid()))
    }

    /** Verifies that when the database fails, its reads and writes report [Outcome.Error] rather than an empty list, a missing task or a refused write. */
    @Test
    fun databaseFailureIsAnError() = runServerTest {
        val taskUseCases = dependency.get()
            .useCases
            .taskUseCases
        dependency.get().database.reset()

        listOf(
            "getTasks" to taskUseCases.getTasks(lastSyncUtc = Instant.fromEpochSeconds(epochSeconds = 0), cursorUtc = null, cursorUuid = null, pageSize = ServerConfigs.value.pageSize),
            "getTask" to taskUseCases.getTask(uuid = FakeData.task.uuid),
            "upsertTask" to taskUseCases.upsertTask(task = FakeData.task)
        ).forEach { (name, outcome) -> assertEquals(expected = Outcome.Error, actual = outcome, message = name) }
    }
}
