package com.app.builder.ui.screen.taskdetail

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlinx.coroutines.flow.first
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextReplacement
import com.app.builder.Dependency.getUserDependency
import com.app.builder.domain.EntityType
import com.app.builder.domain.UserCredentials
import com.app.builder.domain.gateway.UseCases
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

    /**
     * Stores [FakeData.task] as the admin, then logs in with [credentials].
     *
     * @param credentials The credentials to log in with once the task is stored.
     * @return The admin's [UseCases].
     */
    private suspend fun storedTaskUseCases(credentials: UserCredentials = FakeData.adminCredentials): UseCases {
        val useCases = dependency.get()
            .getUserDependency(user = FakeData.adminUser)
            .useCases
        useCases.authenticationUseCases.login(credentials = FakeData.adminCredentials)
        useCases.taskUseCases.upsertTask(task = FakeData.task)
        if (credentials != FakeData.adminCredentials) useCases.authenticationUseCases.login(credentials = credentials)
        return useCases
    }

    /**
     * Renders the task detail screen of [FakeData.task] with an action bar wired to every task detail action, as the task entry provider does.
     *
     * @param useCases The use cases backing the stores.
     * @return The [TaskDetailScreenStore] and the [ActionBarStore] rendered.
     */
    private fun ComposeUiTest.setTaskDetailScreen(useCases: UseCases): Pair<TaskDetailScreenStore, ActionBarStore> {
        val store = TaskDetailScreenStore(state = TaskDetailScreenState(), router = router, taskUseCases = useCases.taskUseCases, taskUuid = FakeData.task.uuid.toString())
        val navigationStore = NavigationStore(state = NavigationState(selected = NavigationRoute.TASK), router = router, authenticationUseCases = useCases.authenticationUseCases)
        val actionBarStore = ActionBarStore(
            state = ActionBarState(),
            router = router,
            authenticationUseCases = useCases.authenticationUseCases,
            entityType = EntityType.TASK,
            onEditClick = { store.send(action = TaskDetailScreenAction.Edit) },
            onSaveClick = { store.send(action = TaskDetailScreenAction.Save) },
            onCancelClick = { store.send(action = TaskDetailScreenAction.Cancel) },
            onDeleteClick = { store.send(action = TaskDetailScreenAction.Delete) }
        )

        setUI {
            TaskDetailScreen(
                actionBarStore = actionBarStore,
                store = store,
                navigationStore = navigationStore
            )
        }

        return store to actionBarStore
    }

    /** Verifies that the task detail screen displays the title and description of the loaded task. */
    @Test
    fun taskDetailScreen() = runUITest {
        val useCases = storedTaskUseCases()

        val store = TaskDetailScreenStore(state = TaskDetailScreenState(), router = router, taskUseCases = useCases.taskUseCases, taskUuid = FakeData.task.uuid.toString())
        val navigationStore = NavigationStore(state = NavigationState(selected = NavigationRoute.TASK), router = router, authenticationUseCases = useCases.authenticationUseCases)
        val actionBarStore = ActionBarStore(state = ActionBarState(), router = router, authenticationUseCases = useCases.authenticationUseCases)

        setUI {
            TaskDetailScreen(
                actionBarStore = actionBarStore,
                store = store,
                navigationStore = navigationStore
            )
        }

        assertTrue(actual = router.backStack.isEmpty())
        waitUntil { store.state.task != null }

        onNodeWithTag(testTag = "action_bar").assertIsDisplayed()
        onNodeWithTag(testTag = "navigation_bar").assertIsDisplayed()
        assertTrue(actual = navigationStore.state.items.first { it.text == "task" }.selected)

        onNodeWithText(text = "task_title").assertIsDisplayed()
        onNodeWithText(text = FakeData.task.title).assertIsDisplayed()
        onNodeWithText(text = "task_description").assertIsDisplayed()
        onNodeWithText(text = FakeData.task.description).assertIsDisplayed()
        onNodeWithTag(testTag = "action_edit").assertDoesNotExist()
        onNodeWithTag(testTag = "action_delete").assertDoesNotExist()
    }

    /** Verifies that a user with write permission views the task read only, with the edit and delete buttons available. */
    @Test
    fun viewTask() = runUITest {
        val (store, _) = setTaskDetailScreen(useCases = storedTaskUseCases())

        waitUntil { store.state.task != null }
        assertFalse(actual = store.state.editing)
        onNodeWithText(text = FakeData.task.title).assertIsNotEnabled()
        onNodeWithText(text = FakeData.task.description).assertIsNotEnabled()
        onNodeWithTag(testTag = "action_edit").assertIsDisplayed()
        onNodeWithTag(testTag = "action_delete").assertIsDisplayed()
        onNodeWithTag(testTag = "action_save").assertDoesNotExist()
        onNodeWithTag(testTag = "action_cancel").assertDoesNotExist()
    }

    /** Verifies that a user with only read permission views the task read only, without any of the edit buttons. */
    @Test
    fun readOnlyTask() = runUITest {
        val (store, actionBarStore) = setTaskDetailScreen(useCases = storedTaskUseCases(credentials = FakeData.credentials))

        waitUntil { store.state.task != null && actionBarStore.state.avatarName == FakeData.user.name }
        onNodeWithText(text = FakeData.task.title).assertIsNotEnabled()
        onNodeWithText(text = FakeData.task.description).assertIsNotEnabled()
        onNodeWithTag(testTag = "action_edit").assertDoesNotExist()
        onNodeWithTag(testTag = "action_save").assertDoesNotExist()
        onNodeWithTag(testTag = "action_cancel").assertDoesNotExist()
        onNodeWithTag(testTag = "action_delete").assertDoesNotExist()
    }

    /** Verifies that the viewed task follows the changes to the stored task. */
    @Test
    fun viewFollowsTask() = runUITest {
        val useCases = storedTaskUseCases()
        val (store, _) = setTaskDetailScreen(useCases = useCases)

        waitUntil { store.state.task != null }
        useCases.taskUseCases.upsertTask(task = FakeData.task.copy(title = "Remote title"))

        waitUntil { store.state.task?.title == "Remote title" }
        onNodeWithText(text = "Remote title").assertIsDisplayed()
    }

    /** Verifies that the edit button makes the task editable, swaps the buttons and stops following the stored task. */
    @Test
    fun editTask() = runUITest {
        val useCases = storedTaskUseCases()
        val (store, _) = setTaskDetailScreen(useCases = useCases)

        waitUntil { store.state.task != null }
        onNodeWithTag(testTag = "action_edit").performClick()
        waitUntil { store.state.editing }

        onNodeWithTag(testTag = "action_edit").assertDoesNotExist()
        onNodeWithTag(testTag = "action_delete").assertDoesNotExist()
        onNodeWithTag(testTag = "action_save").assertIsDisplayed()
        onNodeWithTag(testTag = "action_cancel").assertIsDisplayed()

        onNodeWithText(text = FakeData.task.title).assertIsEnabled().performTextReplacement(text = "New title")
        onNodeWithText(text = FakeData.task.description).assertIsEnabled().performTextReplacement(text = "New description")
        waitForIdle()

        useCases.taskUseCases.upsertTask(task = FakeData.task.copy(title = "Remote title"))
        useCases.taskUseCases.observeTask(uuid = FakeData.task.uuid).first { it?.title == "Remote title" }
        waitForIdle()

        assertEquals(expected = "New title", actual = store.state.task?.title)
        assertEquals(expected = "New description", actual = store.state.task?.description)
    }

    /** Verifies that the save button persists the edits and goes back to viewing the task. */
    @Test
    fun saveTask() = runUITest {
        val useCases = storedTaskUseCases()
        val (store, _) = setTaskDetailScreen(useCases = useCases)

        waitUntil { store.state.task != null }
        onNodeWithTag(testTag = "action_edit").performClick()
        waitUntil { store.state.editing }
        onNodeWithText(text = FakeData.task.title).performTextReplacement(text = "New title")
        onNodeWithText(text = FakeData.task.description).performTextReplacement(text = "New description")
        onNodeWithTag(testTag = "action_save").performClick()
        waitUntil { !store.state.editing }

        val saved = useCases.taskUseCases.observeTask(uuid = FakeData.task.uuid).first { it?.title == "New title" }
        assertEquals(expected = "New description", actual = saved?.description)
        onNodeWithText(text = "New title").assertIsNotEnabled()
        onNodeWithTag(testTag = "action_edit").assertIsDisplayed()
        onNodeWithTag(testTag = "action_save").assertDoesNotExist()
    }

    /** Verifies that the cancel button discards the edits, reloads the stored task and goes back to viewing it. */
    @Test
    fun cancelEdit() = runUITest {
        val useCases = storedTaskUseCases()
        val (store, _) = setTaskDetailScreen(useCases = useCases)

        waitUntil { store.state.task != null }
        onNodeWithTag(testTag = "action_edit").performClick()
        waitUntil { store.state.editing }
        onNodeWithText(text = FakeData.task.title).performTextReplacement(text = "New title")
        onNodeWithTag(testTag = "action_cancel").performClick()
        waitUntil { !store.state.editing }

        waitUntil { store.state.task?.title == FakeData.task.title }
        onNodeWithText(text = FakeData.task.title).assertIsNotEnabled()
        onNodeWithTag(testTag = "action_edit").assertIsDisplayed()
        onNodeWithTag(testTag = "action_cancel").assertDoesNotExist()
        val stored = useCases.taskUseCases.observeTask(uuid = FakeData.task.uuid).first()
        assertEquals(expected = FakeData.task.title, actual = stored?.title)
    }

    /** Verifies that the delete action soft deletes the task and navigates back. */
    @Test
    fun deleteTask() = runUITest {
        val useCases = storedTaskUseCases()

        router.navigate(screen = Screen.TaskList, option = Router.NavOption.ADD)
        router.navigate(screen = Screen.TaskDetail(uuid = FakeData.task.uuid.toString()), option = Router.NavOption.ADD)

        val (store, _) = setTaskDetailScreen(useCases = useCases)

        waitUntil { store.state.task != null }
        onNodeWithTag(testTag = "action_delete").assertIsDisplayed().performClick()
        waitForIdle()

        assertEquals(expected = listOf<Any>(Screen.TaskList), actual = router.backStack.toList())
        val deleted = useCases.taskUseCases.observeTask(uuid = FakeData.task.uuid).first()
        assertNotNull(actual = deleted?.deletedAt)
    }
}
