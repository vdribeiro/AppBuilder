package com.app.builder.ui.screen.devicefiles

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import com.app.builder.data.storage.AppFile
import com.app.builder.test.FakeData
import com.app.builder.test.TestCase
import com.app.builder.ui.component.actionbar.ActionBarState
import com.app.builder.ui.component.actionbar.ActionBarStore
import com.app.builder.ui.component.navigation.NavigationRoute
import com.app.builder.ui.component.navigation.NavigationState
import com.app.builder.ui.component.navigation.NavigationStore

class DeviceFilesScreenTest: TestCase() {

    /** Verifies that a file written by the application is listed with a readable size once the screen refreshes. */
    @Test
    fun listFiles() = runUITest {
        val authenticationUseCases = dependency.get()
            .userGraph
            .useCases
            .authenticationUseCases
        AppFile.TaskPreferences.save { AppFile.ActionBarData(search = "query") }

        val actionBarStore = ActionBarStore(state = ActionBarState(), router = router, authenticationUseCases = authenticationUseCases)
        val navigationStore = NavigationStore(state = NavigationState(selected = NavigationRoute.DEVICE_FILE), router = router, authenticationUseCases = authenticationUseCases)
        val store = DeviceFilesScreenStore(state = DeviceFilesScreenState())

        setUI { DeviceFilesScreen(actionBarStore = actionBarStore, navigationStore = navigationStore, store = store) }

        onNodeWithTag(testTag = "action_bar").assertIsDisplayed()

        waitUntil { store.state.files.any { it.name == AppFile.TaskPreferences.path } }
        val file = store.state.files.first { it.name == AppFile.TaskPreferences.path }
        val description = assertNotNull(actual = file.description)
        assertTrue(actual = Regex(pattern = """^\d+\.\d B( - .+)?$""").matches(input = description), message = description)
    }

    /** Verifies that failing to open a file reports it to the user, and that the report can be dismissed. */
    @Test
    fun openMissingFile() = runUITest {
        val authenticationUseCases = dependency.get()
            .userGraph
            .useCases
            .authenticationUseCases

        val actionBarStore = ActionBarStore(state = ActionBarState(), router = router, authenticationUseCases = authenticationUseCases)
        val navigationStore = NavigationStore(state = NavigationState(selected = NavigationRoute.DEVICE_FILE), router = router, authenticationUseCases = authenticationUseCases)
        val store = DeviceFilesScreenStore(state = DeviceFilesScreenState())

        setUI { DeviceFilesScreen(actionBarStore = actionBarStore, navigationStore = navigationStore, store = store) }
        assertNull(actual = store.state.message)

        store.send(action = DeviceFilesScreenAction.OpenFile(path = "${FakeData.task.uuid}.missing"))
        waitUntil { store.state.message != null }

        assertEquals(expected = "unable_to_open_file", actual = store.state.message)
        onNodeWithText(text = "unable_to_open_file").assertIsDisplayed()

        store.send(action = DeviceFilesScreenAction.DismissMessage)
        waitForIdle()

        assertNull(actual = store.state.message)
        onNodeWithText(text = "unable_to_open_file").assertDoesNotExist()
    }
}
