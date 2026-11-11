package com.app.builder.ui.navigation.task

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.flow.first
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.app.builder.Dependency.getUserDependency
import com.app.builder.core.security.uuid
import com.app.builder.data.storage.AppFile
import com.app.builder.domain.Task
import com.app.builder.test.FakeData
import com.app.builder.test.TestCase
import com.app.builder.test.count
import com.app.builder.ui.component.filters.FiltersState
import com.app.builder.ui.component.filters.FiltersStore
import com.app.builder.ui.component.session.SessionState
import com.app.builder.ui.component.session.SessionStore
import com.app.builder.ui.navigation.Router
import com.app.builder.ui.navigation.Screen

class TaskListScreenTest: TestCase() {

    /** Verifies that the task list displays a seeded task and that tapping its card navigates to its detail screen. */
    @Test
    fun taskScreen() = runUITest {
        val authenticatedUseCases = dependency.get()
            .getUserDependency(account = FakeData.adminAccount)
            .useCases
        authenticatedUseCases.authenticationUseCases.login(credentials = FakeData.adminCredentials)
        authenticatedUseCases.taskUseCases.upsertTask(task = FakeData.task)

        // The list store only emits once TaskPreferences holds data, and it is the filters store that seeds that file, so it has to be wired up as the provider does.
        val properties = Task.Property.entries.map { it.name }.toPersistentList()
        val defaults = AppFile.ActionBarData(
            search = "",
            sortProperty = Task.Property.MODIFIED_AT.name,
            sortAscending = false,
            visibleProperties = properties,
            searchableProperties = properties,
        )
        val sessionStore = SessionStore(state = SessionState(), authenticationUseCases = authenticatedUseCases.authenticationUseCases)
        val filtersStore = FiltersStore(
            state = FiltersState(visibleProperties = properties, searchableProperties = properties),
            storageFile = AppFile.TaskPreferences,
            defaults = defaults
        )
        val taskListScreenStore = TaskListScreenStore(state = TaskListScreenState(), router = router, taskUseCases = authenticatedUseCases.taskUseCases)

        setUI {
            TaskListScreen(
                sessionStore = sessionStore,
                filtersStore = filtersStore,
                store = taskListScreenStore
            )
        }

        assertTrue(actual = router.backStack.isEmpty())
        waitUntil { taskListScreenStore.state.tasks.size == 1 }

        onNodeWithTag(testTag = "action_bar").assertIsDisplayed()
        onNodeWithTag(testTag = "navigation_bar").assertIsDisplayed()
        onNodeWithText(text = "task").assertIsSelected()

        onNodeWithTag(testTag = "task_list").assertIsDisplayed()
        onNodeWithTag(testTag = "task_list").count(count = 1)

        onNodeWithTag(testTag = "task_row_0").assertIsDisplayed().performClick()
        val taskDetailScreen = Screen.TaskDetail(uuid = taskListScreenStore.state.tasks.first().uuid)
        assertEquals(expected = listOf(taskDetailScreen), actual = router.backStack.toList())
        waitUntil { taskListScreenStore.state.tasks.first().selected }
    }

    /** Verifies that the selected task follows the task detail screen in the backstack, however it got there. */
    @Test
    fun selectionFollowsDetailScreen() = runUITest {
        val authenticatedUseCases = dependency.get()
            .getUserDependency(account = FakeData.adminAccount)
            .useCases
        authenticatedUseCases.authenticationUseCases.login(credentials = FakeData.adminCredentials)
        val otherTask = FakeData.task.copy(uuid = uuid(), title = "Other")
        authenticatedUseCases.taskUseCases.upsertTask(task = FakeData.task)
        authenticatedUseCases.taskUseCases.upsertTask(task = otherTask)

        // The list store only emits once TaskPreferences holds data, and it is the filters store that seeds that file, so it has to be wired up as the provider does.
        val properties = Task.Property.entries.map { it.name }.toPersistentList()
        val defaults = AppFile.ActionBarData(
            search = "",
            sortProperty = Task.Property.MODIFIED_AT.name,
            sortAscending = false,
            visibleProperties = properties,
            searchableProperties = properties,
        )
        val sessionStore = SessionStore(state = SessionState(), authenticationUseCases = authenticatedUseCases.authenticationUseCases)
        val filtersStore = FiltersStore(
            state = FiltersState(visibleProperties = properties, searchableProperties = properties),
            storageFile = AppFile.TaskPreferences,
            defaults = defaults
        )
        val taskListScreenStore = TaskListScreenStore(state = TaskListScreenState(), router = router, taskUseCases = authenticatedUseCases.taskUseCases)

        setUI {
            TaskListScreen(
                sessionStore = sessionStore,
                filtersStore = filtersStore,
                store = taskListScreenStore
            )
        }
        waitUntil { taskListScreenStore.state.tasks.size == 2 }
        fun selectedUuids() = taskListScreenStore.state.tasks.filter { it.selected }.map { it.uuid }

        router.navigate(screen = Screen.TaskList)
        router.navigate(screen = Screen.TaskDetail(uuid = otherTask.uuid.toString()), option = Router.NavOption.REPLACE_LAST)
        waitUntil { selectedUuids() == listOf(otherTask.uuid.toString()) }

        router.navigate(screen = Screen.TaskDetail(uuid = FakeData.task.uuid.toString()), option = Router.NavOption.REPLACE_LAST)
        waitUntil { selectedUuids() == listOf(FakeData.task.uuid.toString()) }

        router.navigate(screen = Screen.TaskDetail(uuid = uuid().toString(), new = true), option = Router.NavOption.REPLACE_LAST)
        waitUntil { selectedUuids().isEmpty() }

        router.navigate(screen = Screen.TaskDetail(uuid = otherTask.uuid.toString()), option = Router.NavOption.REPLACE_LAST)
        waitUntil { selectedUuids() == listOf(otherTask.uuid.toString()) }
        router.back()
        waitUntil { selectedUuids().isEmpty() }
    }

    /** Verifies that the add action opens the detail screen of a new task without storing it. */
    @Test
    fun addTask() = runUITest {
        val authenticatedUseCases = dependency.get()
            .getUserDependency(account = FakeData.adminAccount)
            .useCases
        authenticatedUseCases.authenticationUseCases.login(credentials = FakeData.adminCredentials)

        val properties = Task.Property.entries.map { it.name }.toPersistentList()
        val defaults = AppFile.ActionBarData(
            search = "",
            sortProperty = Task.Property.MODIFIED_AT.name,
            sortAscending = false,
            visibleProperties = properties,
            searchableProperties = properties,
        )
        val sessionStore = SessionStore(state = SessionState(), authenticationUseCases = authenticatedUseCases.authenticationUseCases)
        val filtersStore = FiltersStore(
            state = FiltersState(visibleProperties = properties, searchableProperties = properties),
            storageFile = AppFile.TaskPreferences,
            defaults = defaults
        )
        val taskListScreenStore = TaskListScreenStore(state = TaskListScreenState(), router = router, taskUseCases = authenticatedUseCases.taskUseCases)

        setUI {
            TaskListScreen(
                sessionStore = sessionStore,
                filtersStore = filtersStore,
                store = taskListScreenStore
            )
        }

        onNodeWithTag(testTag = "action_add").assertIsDisplayed().performClick()
        waitUntil { router.backStack.isNotEmpty() }

        val screen = assertIs<Screen.TaskDetail>(value = router.backStack.single())
        assertTrue(actual = screen.new)
        assertTrue(actual = authenticatedUseCases.taskUseCases.observeTasks().first().isEmpty())
    }
}
