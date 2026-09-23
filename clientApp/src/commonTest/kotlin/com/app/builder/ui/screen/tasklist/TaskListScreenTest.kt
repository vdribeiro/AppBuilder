package com.app.builder.ui.screen.tasklist

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import kotlinx.collections.immutable.toPersistentList
import com.app.builder.Dependency.getUserDependency
import com.app.builder.data.storage.AppFile
import com.app.builder.domain.EntityType
import com.app.builder.domain.Task
import com.app.builder.test.FakeData
import com.app.builder.test.TestCase
import com.app.builder.test.count
import com.app.builder.ui.component.actionbar.ActionBarState
import com.app.builder.ui.component.actionbar.ActionBarStore
import com.app.builder.ui.component.bar.ActionBarLayout
import com.app.builder.ui.component.bar.ActionBarMode
import com.app.builder.ui.component.navigation.NavigationRoute
import com.app.builder.ui.component.navigation.NavigationState
import com.app.builder.ui.component.navigation.NavigationStore
import com.app.builder.ui.navigation.Screen

class TaskListScreenTest: TestCase() {

    /** Verifies that the task list displays a seeded task and that tapping its card navigates to its detail screen. */
    @Test
    fun taskScreen() = runUITest {
        val authenticatedUseCases = dependency.get()
            .getUserDependency(user = FakeData.adminUser)
            .useCases
        authenticatedUseCases.authenticationUseCases.login(credentials = FakeData.adminCredentials)
        authenticatedUseCases.taskUseCases.upsertTask(task = FakeData.task)

        val navigationStore = NavigationStore(state = NavigationState(selected = NavigationRoute.TASK), router = router, authenticationUseCases = authenticatedUseCases.authenticationUseCases)
        // The list store only emits once TaskPreferences holds data, and it is the action bar store that seeds that file, so it has to be wired up as the provider does.
        val properties = Task.Property.entries.map { it.name }.toPersistentList()
        val defaults = AppFile.ActionBarData(
            mode = ActionBarMode.DEFAULT.name,
            search = "",
            sortProperty = Task.Property.MODIFIED_AT.name,
            sortAscending = false,
            visibleProperties = properties,
            searchableProperties = properties,
        )
        val actionBarStore = ActionBarStore(
            state = ActionBarState(title = "tasks", layout = ActionBarLayout.LIST, visibleProperties = properties, searchableProperties = properties),
            router = router,
            authenticationUseCases = authenticatedUseCases.authenticationUseCases,
            storageFile = AppFile.TaskPreferences,
            defaults = defaults,
            entityType = EntityType.TASK,
        )
        val taskListScreenStore = TaskListScreenStore(state = TaskListScreenState(), router = router, taskUseCases = authenticatedUseCases.taskUseCases)

        setUI {
            TaskListScreen(
                actionBarStore = actionBarStore,
                navigationStore = navigationStore,
                store = taskListScreenStore
            )
        }

        assertTrue(actual = router.backStack.isEmpty())
        waitUntil { taskListScreenStore.state.tasks.size == 1 }

        onNodeWithTag(testTag = "action_bar").assertIsDisplayed()
        onNodeWithTag(testTag = "navigation_bar").assertIsDisplayed()
        onNodeWithText(text = "task").assertIsSelected()

        onNodeWithText(text = "tasks").assertIsDisplayed()
        onNodeWithTag(testTag = "task_list").assertIsDisplayed()
        onNodeWithTag(testTag = "task_list").count(count = 1)

        onNodeWithTag(testTag = "task_card_0").assertIsDisplayed().performClick()
        val taskDetailScreen = Screen.TaskDetail(uuid = taskListScreenStore.state.tasks.first().uuid)
        assertEquals(expected = listOf(taskDetailScreen), actual = router.backStack.toList())
    }
}
