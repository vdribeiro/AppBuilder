package com.app.builder.ui.screen.tasklist

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.app.builder.Dependency.getUserDependency
import com.app.builder.test.FakeData
import com.app.builder.test.TestCase
import com.app.builder.test.count
import com.app.builder.ui.component.navigation.NavigationRoute
import com.app.builder.ui.component.navigation.NavigationState
import com.app.builder.ui.component.navigation.NavigationStore
import com.app.builder.ui.component.useravatar.UserAvatarState
import com.app.builder.ui.component.useravatar.UserAvatarStore
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
        val userAvatarStore = UserAvatarStore(state = UserAvatarState(), authenticationUseCases = authenticatedUseCases.authenticationUseCases)
        val taskListScreenStore = TaskListScreenStore(state = TaskListScreenState(), router = router, authenticationUseCases = authenticatedUseCases.authenticationUseCases, taskUseCases = authenticatedUseCases.taskUseCases)

        setUI {
            TaskListScreen(
                navigationStore = navigationStore,
                userAvatarStore = userAvatarStore,
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
