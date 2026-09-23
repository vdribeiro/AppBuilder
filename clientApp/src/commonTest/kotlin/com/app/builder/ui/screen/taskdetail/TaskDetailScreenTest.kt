package com.app.builder.ui.screen.taskdetail

import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import com.app.builder.Dependency.getUserDependency
import com.app.builder.test.FakeData
import com.app.builder.test.TestCase
import com.app.builder.ui.component.actionbar.ActionBarState
import com.app.builder.ui.component.actionbar.ActionBarStore
import com.app.builder.ui.component.bar.ActionBarLayout
import com.app.builder.ui.component.navigation.NavigationRoute
import com.app.builder.ui.component.navigation.NavigationState
import com.app.builder.ui.component.navigation.NavigationStore

class TaskDetailScreenTest: TestCase() {

    /** Verifies that the task detail screen displays the title and description of the loaded task. */
    @Test
    fun taskDetailScreen() = runUITest {
        val authenticatedUseCases = dependency.get()
            .getUserDependency(user = FakeData.adminUser)
            .useCases
        authenticatedUseCases.authenticationUseCases.login(credentials = FakeData.adminCredentials)
        authenticatedUseCases.taskUseCases.upsertTask(task = FakeData.task)

        val store = TaskDetailScreenStore(state = TaskDetailScreenState(), taskUseCases = authenticatedUseCases.taskUseCases, taskUuid = FakeData.task.uuid.toString())
        val navigationStore = NavigationStore(state = NavigationState(selected = NavigationRoute.TASK), router = router, authenticationUseCases = authenticatedUseCases.authenticationUseCases)
        val actionBarStore = ActionBarStore(state = ActionBarState(title = "task", layout = ActionBarLayout.DETAIL), router = router, authenticationUseCases = authenticatedUseCases.authenticationUseCases)

        setUI {
            TaskDetailScreen(
                actionBarStore = actionBarStore,
                store = store,
                navigationStore = navigationStore
            )
        }

        assertTrue(actual = router.backStack.isEmpty())
        assertNotNull(actual = store.state.task)

        onNodeWithTag(testTag = "action_bar").assertIsDisplayed()
        onNodeWithTag(testTag = "navigation_bar").assertIsDisplayed()
        assertTrue(actual = navigationStore.state.items.first { it.text == "task" }.selected)

        onNodeWithText(text = "task_title").assertIsDisplayed()
        onNodeWithText(text = FakeData.task.title).assertIsDisplayed()
        onNodeWithText(text = "task_description").assertIsDisplayed()
        onNodeWithText(text = FakeData.task.description).assertIsDisplayed()
    }
}
