package com.app.builder.ui.screen.taskdetail

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextReplacement
import kotlinx.coroutines.flow.first
import com.app.builder.Dependency.getUserDependency
import com.app.builder.domain.EntityType
import com.app.builder.test.FakeData
import com.app.builder.test.TestCase
import com.app.builder.ui.component.actionbar.ActionBarState
import com.app.builder.ui.component.actionbar.ActionBarStore
import com.app.builder.ui.component.navigation.NavigationRoute
import com.app.builder.ui.component.navigation.NavigationState
import com.app.builder.ui.component.navigation.NavigationStore
import com.app.builder.ui.navigation.Router
import com.app.builder.ui.navigation.Screen

class TaskDetailScreenTest: TestCase() {

    /** Verifies that the task detail screen displays the title and description of the loaded task. */
    @Test
    fun taskDetailScreen() = runUITest {
        val authenticatedUseCases = dependency.get()
            .getUserDependency(user = FakeData.adminUser)
            .useCases
        authenticatedUseCases.authenticationUseCases.login(credentials = FakeData.adminCredentials)
        authenticatedUseCases.taskUseCases.upsertTask(task = FakeData.task)

        val store = TaskDetailScreenStore(state = TaskDetailScreenState(), router = router, taskUseCases = authenticatedUseCases.taskUseCases, taskUuid = FakeData.task.uuid.toString())
        val navigationStore = NavigationStore(state = NavigationState(selected = NavigationRoute.TASK), router = router, authenticationUseCases = authenticatedUseCases.authenticationUseCases)
        val actionBarStore = ActionBarStore(state = ActionBarState(title = "task"), router = router, authenticationUseCases = authenticatedUseCases.authenticationUseCases)

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
        onNodeWithTag(testTag = "action_delete").assertDoesNotExist()
    }

    /** Verifies that editing the title and description updates the task held in the store. */
    @Test
    fun editTask() = runUITest {
        val authenticatedUseCases = dependency.get()
            .getUserDependency(user = FakeData.adminUser)
            .useCases
        authenticatedUseCases.authenticationUseCases.login(credentials = FakeData.adminCredentials)
        authenticatedUseCases.taskUseCases.upsertTask(task = FakeData.task)

        val store = TaskDetailScreenStore(state = TaskDetailScreenState(), router = router, taskUseCases = authenticatedUseCases.taskUseCases, taskUuid = FakeData.task.uuid.toString())
        val navigationStore = NavigationStore(state = NavigationState(selected = NavigationRoute.TASK), router = router, authenticationUseCases = authenticatedUseCases.authenticationUseCases)
        val actionBarStore = ActionBarStore(state = ActionBarState(title = "task"), router = router, authenticationUseCases = authenticatedUseCases.authenticationUseCases)

        setUI {
            TaskDetailScreen(
                actionBarStore = actionBarStore,
                store = store,
                navigationStore = navigationStore
            )
        }

        onNodeWithText(text = FakeData.task.title).performTextReplacement(text = "New title")
        onNodeWithText(text = FakeData.task.description).performTextReplacement(text = "New description")
        waitForIdle()

        assertEquals(expected = "New title", actual = store.state.task?.title)
        assertEquals(expected = "New description", actual = store.state.task?.description)
    }

    /** Verifies that the delete action soft deletes the task and navigates back. */
    @Test
    fun deleteTask() = runUITest {
        val authenticatedUseCases = dependency.get()
            .getUserDependency(user = FakeData.adminUser)
            .useCases
        authenticatedUseCases.authenticationUseCases.login(credentials = FakeData.adminCredentials)
        authenticatedUseCases.taskUseCases.upsertTask(task = FakeData.task)

        router.navigate(screen = Screen.TaskList, option = Router.NavOption.ADD)
        router.navigate(screen = Screen.TaskDetail(uuid = FakeData.task.uuid.toString()), option = Router.NavOption.ADD)

        val store = TaskDetailScreenStore(state = TaskDetailScreenState(), router = router, taskUseCases = authenticatedUseCases.taskUseCases, taskUuid = FakeData.task.uuid.toString())
        val navigationStore = NavigationStore(state = NavigationState(selected = NavigationRoute.TASK), router = router, authenticationUseCases = authenticatedUseCases.authenticationUseCases)
        val actionBarStore = ActionBarStore(
            state = ActionBarState(title = "task"),
            router = router,
            authenticationUseCases = authenticatedUseCases.authenticationUseCases,
            entityType = EntityType.TASK,
            onDeleteClick = { store.send(action = TaskDetailScreenAction.Delete) }
        )

        setUI {
            TaskDetailScreen(
                actionBarStore = actionBarStore,
                store = store,
                navigationStore = navigationStore
            )
        }

        waitUntil { store.state.task != null }
        onNodeWithTag(testTag = "action_delete").assertIsDisplayed().performClick()
        waitForIdle()

        assertEquals(expected = listOf<Any>(Screen.TaskList), actual = router.backStack.toList())
        val deleted = authenticatedUseCases.taskUseCases.observeTask(uuid = FakeData.task.uuid).first()
        assertNotNull(actual = deleted?.deletedAt)
    }
}
