package com.app.builder.domain.gateway.task

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.flow.firstOrNull
import com.app.builder.Dependency.getUserDependency
import com.app.builder.core.locale.now
import com.app.builder.core.security.uuid
import com.app.builder.test.FakeData
import com.app.builder.test.TestCase

class TaskUseCasesTest: TestCase() {

    /** Verifies that upserting a task makes it observable both individually and in the task list. */
    @Test
    fun crudTask() = runUnitTest {
        val taskUseCases = dependency.get()
            .getUserDependency(user = FakeData.adminUser)
            .useCases
            .taskUseCases

        assertTrue(actual = taskUseCases.observeTasks(filter = TaskFilter.Active).firstOrNull().orEmpty().isEmpty())
        taskUseCases.upsertTask(task = FakeData.task)
        assertEquals(expected = listOf(element = FakeData.task), actual = taskUseCases.observeTasks(filter = TaskFilter.Active).firstOrNull().orEmpty())
        assertEquals(expected = FakeData.task, actual = taskUseCases.observeTask(uuid = FakeData.task.uuid).firstOrNull())
    }

    /** Verifies that the active filter observes only the tasks that are not deleted, and the deleted filter only the deleted ones. */
    @Test
    fun filterTasks() = runUnitTest {
        val taskUseCases = dependency.get()
            .getUserDependency(user = FakeData.adminUser)
            .useCases
            .taskUseCases
        val deletedTask = FakeData.task.copy(uuid = uuid(), deletedAt = now())

        taskUseCases.upsertTask(task = FakeData.task)
        taskUseCases.upsertTask(task = deletedTask)

        assertEquals(expected = listOf(element = FakeData.task), actual = taskUseCases.observeTasks(filter = TaskFilter.Active).firstOrNull().orEmpty())
        assertEquals(expected = listOf(element = deletedTask), actual = taskUseCases.observeTasks(filter = TaskFilter.Deleted).firstOrNull().orEmpty())
    }
}
