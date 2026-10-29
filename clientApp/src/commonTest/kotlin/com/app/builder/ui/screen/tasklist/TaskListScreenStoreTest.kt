package com.app.builder.ui.screen.tasklist

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.time.Instant
import kotlinx.coroutines.flow.first
import com.app.builder.Dependency.getUserDependency
import com.app.builder.data.storage.AppFile
import com.app.builder.domain.Task
import com.app.builder.domain.Task.Property
import com.app.builder.test.FakeData
import com.app.builder.test.TestCase
import com.app.builder.ui.component.list.TaskItem

class TaskListScreenStoreTest: TestCase() {

    /** A live task, modified in 2020. */
    private val alpha = Task(
        modifiedAt = Instant.parse(input = "2020-01-01T00:00:00Z"),
        title = "Alpha",
        description = "first",
    )

    /** A live task, modified in 2021. */
    private val beta = Task(
        modifiedAt = Instant.parse(input = "2021-06-01T00:00:00Z"),
        title = "Beta",
        description = "second",
    )

    /** A deleted task, modified in 2019 and deleted in 2021, which the list never shows. */
    private val gamma = Task(
        modifiedAt = Instant.parse(input = "2019-01-01T00:00:00Z"),
        deletedAt = Instant.parse(input = "2021-01-01T00:00:00Z"),
        title = "Gamma",
        description = "third",
    )

    /** Every [Property] name, so every property is searchable and visible unless a test narrows it. */
    private val allProperties = Property.entries.map { it.name }

    /**
     * Seeds [alpha], [beta] and [gamma], saves [preferences] as the task preferences, and waits for the store to list [count] tasks.
     *
     * @param preferences The action bar data the store filters, sorts and projects with.
     * @param count The number of tasks the store is expected to list.
     * @return The listed [TaskItem]s, in display order.
     */
    private suspend fun listedTasks(preferences: AppFile.ActionBarData, count: Int): List<TaskItem> {
        val authenticatedUseCases = dependency.get()
            .getUserDependency(user = FakeData.adminUser)
            .useCases
        authenticatedUseCases.authenticationUseCases.login(credentials = FakeData.adminCredentials)
        authenticatedUseCases.taskUseCases.upsertTask(task = alpha)
        authenticatedUseCases.taskUseCases.upsertTask(task = beta)
        authenticatedUseCases.taskUseCases.upsertTask(task = gamma)
        AppFile.TaskPreferences.save { preferences }

        val store = TaskListScreenStore(state = TaskListScreenState(), router = router, taskUseCases = authenticatedUseCases.taskUseCases)
        return store.stateFlow.first { it.tasks.size == count }.tasks
    }

    /**
     * Lists the tasks matching [search] within [searchableProperties].
     *
     * @param search The search text.
     * @param searchableProperties The property names to search within.
     * @return The titles of the matching tasks.
     */
    private suspend fun search(search: String, vararg searchableProperties: Property): List<String?> = listedTasks(
        preferences = AppFile.ActionBarData(
            search = search,
            searchableProperties = searchableProperties.map { it.name },
            visibleProperties = allProperties,
        ),
        count = 1
    ).map { it.title }

    /**
     * Lists every task sorted by [sortProperty].
     *
     * @param sortProperty The property name to sort by.
     * @param sortAscending Whether to sort in ascending order.
     * @return The titles of the tasks, in display order.
     */
    private suspend fun sort(sortProperty: String, sortAscending: Boolean): List<String?> = listedTasks(
        preferences = AppFile.ActionBarData(
            sortProperty = sortProperty,
            sortAscending = sortAscending,
            visibleProperties = allProperties,
        ),
        count = 2
    ).map { it.title }

    /** Verifies that a search matches the task's title, ignoring case. */
    @Test
    fun searchMatchesTitle() = runUnitTest {
        assertEquals(expected = listOf("Alpha"), actual = search(search = "ALP", Property.TITLE))
    }

    /** Verifies that a search matches the task's description. */
    @Test
    fun searchMatchesDescription() = runUnitTest {
        assertEquals(expected = listOf("Beta"), actual = search(search = "second", Property.DESCRIPTION))
    }

    /** Verifies that a search matches the task's modification time. */
    @Test
    fun searchMatchesModifiedAt() = runUnitTest {
        assertEquals(expected = listOf("Alpha"), actual = search(search = "2020", Property.MODIFIED_AT))
    }

    /** Verifies that a property the search does not support never matches, leaving the supported ones to decide. */
    @Test
    fun searchIgnoresUnsupportedProperties() = runUnitTest {
        assertEquals(expected = listOf("Beta"), actual = search(search = "Beta", Property.STATE, Property.TITLE))
    }

    /** Verifies that tasks are sorted by modification time in both directions. */
    @Test
    fun sortByModifiedAt() = runUnitTest {
        assertEquals(expected = listOf("Alpha", "Beta"), actual = sort(sortProperty = Property.MODIFIED_AT.name, sortAscending = true))
        assertEquals(expected = listOf("Beta", "Alpha"), actual = sort(sortProperty = Property.MODIFIED_AT.name, sortAscending = false))
    }

    /** Verifies that tasks are sorted by title. */
    @Test
    fun sortByTitle() = runUnitTest {
        assertEquals(expected = listOf("Beta", "Alpha"), actual = sort(sortProperty = Property.TITLE.name, sortAscending = false))
    }

    /** Verifies that tasks are sorted by description. */
    @Test
    fun sortByDescription() = runUnitTest {
        assertEquals(expected = listOf("Alpha", "Beta"), actual = sort(sortProperty = Property.DESCRIPTION.name, sortAscending = true))
    }

    /** Verifies that a property the sort does not support lists every task without sorting them. */
    @Test
    fun sortIgnoresUnsupportedProperties() = runUnitTest {
        assertEquals(expected = setOf("Alpha", "Beta"), actual = sort(sortProperty = Property.STATE.name, sortAscending = true).toSet())
    }

    /** Verifies that only the visible properties are projected onto the listed items. */
    @Test
    fun hiddenPropertiesAreLeftOut() = runUnitTest {
        val items = listedTasks(preferences = AppFile.ActionBarData(visibleProperties = listOf(Property.TITLE.name)), count = 2)

        items.forEach { item ->
            assertNull(actual = item.modifiedAt)
            assertNull(actual = item.deletedAt)
            assertNull(actual = item.description)
        }
        assertEquals(expected = setOf("Alpha", "Beta"), actual = items.map { it.title }.toSet())
    }

    /** Verifies that deleted tasks are not listed. */
    @Test
    fun deletedTasksAreLeftOut() = runUnitTest {
        val items = listedTasks(preferences = AppFile.ActionBarData(visibleProperties = allProperties), count = 2)

        assertEquals(expected = setOf("Alpha", "Beta"), actual = items.map { it.title }.toSet())
    }
}
