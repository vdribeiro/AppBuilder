package com.app.builder.ui.screen.taskdetail

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
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

        val store = TaskDetailScreenStore(state = TaskDetailScreenState(), router = router, authenticationUseCases = authenticatedUseCases.authenticationUseCases, taskUseCases = authenticatedUseCases.taskUseCases, taskUuid = FakeData.task.uuid.toString())
        val navigationStore = NavigationStore(state = NavigationState(selected = NavigationRoute.TASK), router = router, authenticationUseCases = authenticatedUseCases.authenticationUseCases)
        val actionBarStore = ActionBarStore(state = ActionBarState(), router = router, authenticationUseCases = authenticatedUseCases.authenticationUseCases)

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

        val store = TaskDetailScreenStore(state = TaskDetailScreenState(), router = router, authenticationUseCases = authenticatedUseCases.authenticationUseCases, taskUseCases = authenticatedUseCases.taskUseCases, taskUuid = FakeData.task.uuid.toString())
        val navigationStore = NavigationStore(state = NavigationState(selected = NavigationRoute.TASK), router = router, authenticationUseCases = authenticatedUseCases.authenticationUseCases)
        val actionBarStore = ActionBarStore(state = ActionBarState(), router = router, authenticationUseCases = authenticatedUseCases.authenticationUseCases)

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

        assertEquals(expected = "New title", actual = store.state.title)
        assertEquals(expected = "New description", actual = store.state.description)
        assertEquals(expected = FakeData.task.title, actual = store.state.task?.title)
        assertEquals(expected = FakeData.task.description, actual = store.state.task?.description)
    }

    /** Verifies that a user with only read permission sees the title and description disabled and no save button. */
    @Test
    fun readOnlyTask() = runUITest {
        val authenticatedUseCases = dependency.get()
            .getUserDependency(user = FakeData.adminUser)
            .useCases
        authenticatedUseCases.authenticationUseCases.login(credentials = FakeData.adminCredentials)
        authenticatedUseCases.taskUseCases.upsertTask(task = FakeData.task)
        authenticatedUseCases.authenticationUseCases.login(credentials = FakeData.credentials)

        val store = TaskDetailScreenStore(state = TaskDetailScreenState(), router = router, authenticationUseCases = authenticatedUseCases.authenticationUseCases, taskUseCases = authenticatedUseCases.taskUseCases, taskUuid = FakeData.task.uuid.toString())
        val navigationStore = NavigationStore(state = NavigationState(selected = NavigationRoute.TASK), router = router, authenticationUseCases = authenticatedUseCases.authenticationUseCases)
        val actionBarStore = ActionBarStore(
            state = ActionBarState(),
            router = router,
            authenticationUseCases = authenticatedUseCases.authenticationUseCases,
            entityType = EntityType.TASK,
            onSaveClick = { store.send(action = TaskDetailScreenAction.Save) },
            onDeleteClick = { store.send(action = TaskDetailScreenAction.Delete) }
        )

        setUI {
            TaskDetailScreen(
                actionBarStore = actionBarStore,
                store = store,
                navigationStore = navigationStore
            )
        }

        waitUntil { store.state.task != null && actionBarStore.state.avatarName == FakeData.user.name }
        assertFalse(actual = store.state.editable)
        onNodeWithText(text = FakeData.task.title).assertIsNotEnabled()
        onNodeWithText(text = FakeData.task.description).assertIsNotEnabled()
        onNodeWithTag(testTag = "action_save").assertDoesNotExist()
        onNodeWithTag(testTag = "action_delete").assertDoesNotExist()
    }

    /** Verifies that the save action persists the edited title and description. */
    @Test
    fun saveTask() = runUITest {
        val authenticatedUseCases = dependency.get()
            .getUserDependency(user = FakeData.adminUser)
            .useCases
        authenticatedUseCases.authenticationUseCases.login(credentials = FakeData.adminCredentials)
        authenticatedUseCases.taskUseCases.upsertTask(task = FakeData.task)

        val store = TaskDetailScreenStore(state = TaskDetailScreenState(), router = router, authenticationUseCases = authenticatedUseCases.authenticationUseCases, taskUseCases = authenticatedUseCases.taskUseCases, taskUuid = FakeData.task.uuid.toString())
        val navigationStore = NavigationStore(state = NavigationState(selected = NavigationRoute.TASK), router = router, authenticationUseCases = authenticatedUseCases.authenticationUseCases)
        val actionBarStore = ActionBarStore(
            state = ActionBarState(),
            router = router,
            authenticationUseCases = authenticatedUseCases.authenticationUseCases,
            entityType = EntityType.TASK,
            onSaveClick = { store.send(action = TaskDetailScreenAction.Save) },
            onDeleteClick = { store.send(action = TaskDetailScreenAction.Delete) }
        )

        setUI {
            TaskDetailScreen(
                actionBarStore = actionBarStore,
                store = store,
                navigationStore = navigationStore
            )
        }

        waitUntil { store.state.task != null && store.state.editable }
        onNodeWithText(text = FakeData.task.title).assertIsEnabled().performTextReplacement(text = "New title")
        onNodeWithText(text = FakeData.task.description).assertIsEnabled().performTextReplacement(text = "New description")
        onNodeWithTag(testTag = "action_save").assertIsDisplayed().performClick()
        waitForIdle()

        val saved = authenticatedUseCases.taskUseCases.observeTask(uuid = FakeData.task.uuid).first { it?.title == "New title" }
        assertEquals(expected = "New description", actual = saved?.description)
        assertEquals(expected = "New title", actual = store.state.title)
        assertEquals(expected = "New description", actual = store.state.description)
    }

    /** Verifies that unsaved edits are kept when the task changes while it's being edited. */
    @Test
    fun keepEditsOnTaskChange() = runUITest {
        val authenticatedUseCases = dependency.get()
            .getUserDependency(user = FakeData.adminUser)
            .useCases
        authenticatedUseCases.authenticationUseCases.login(credentials = FakeData.adminCredentials)
        authenticatedUseCases.taskUseCases.upsertTask(task = FakeData.task)

        val store = TaskDetailScreenStore(state = TaskDetailScreenState(), router = router, authenticationUseCases = authenticatedUseCases.authenticationUseCases, taskUseCases = authenticatedUseCases.taskUseCases, taskUuid = FakeData.task.uuid.toString())
        val navigationStore = NavigationStore(state = NavigationState(selected = NavigationRoute.TASK), router = router, authenticationUseCases = authenticatedUseCases.authenticationUseCases)
        val actionBarStore = ActionBarStore(state = ActionBarState(), router = router, authenticationUseCases = authenticatedUseCases.authenticationUseCases)

        setUI {
            TaskDetailScreen(
                actionBarStore = actionBarStore,
                store = store,
                navigationStore = navigationStore
            )
        }

        waitUntil { store.state.task != null && store.state.editable }
        onNodeWithText(text = FakeData.task.title).performTextReplacement(text = "New title")
        waitForIdle()

        authenticatedUseCases.taskUseCases.upsertTask(task = FakeData.task.copy(title = "Remote title"))
        waitUntil { store.state.task?.title == "Remote title" }

        assertEquals(expected = "New title", actual = store.state.title)
        onNodeWithText(text = "New title").assertIsDisplayed()
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

        val store = TaskDetailScreenStore(state = TaskDetailScreenState(), router = router, authenticationUseCases = authenticatedUseCases.authenticationUseCases, taskUseCases = authenticatedUseCases.taskUseCases, taskUuid = FakeData.task.uuid.toString())
        val navigationStore = NavigationStore(state = NavigationState(selected = NavigationRoute.TASK), router = router, authenticationUseCases = authenticatedUseCases.authenticationUseCases)
        val actionBarStore = ActionBarStore(
            state = ActionBarState(),
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
