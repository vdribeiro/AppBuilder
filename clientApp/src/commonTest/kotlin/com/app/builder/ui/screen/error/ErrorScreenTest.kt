package com.app.builder.ui.screen.error

import kotlin.test.Test
import kotlin.test.assertEquals
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.navigation3.runtime.NavKey
import com.app.builder.core.config.ClientFlags
import com.app.builder.test.TestCase
import com.app.builder.ui.AppState
import com.app.builder.ui.LocalAppState
import com.app.builder.ui.navigation.Screen

class ErrorScreenTest: TestCase() {

    /**
     * Renders the error screen on top of a back stack that already holds the home and task list screens, then taps its back button.
     *
     * @param error Whether the screen was reached because of an error.
     * @param appState The application state the screen is rendered in.
     * @return The back stack once the back button was handled.
     */
    private fun ComposeUiTest.navigateBack(error: Boolean, appState: AppState): List<NavKey> {
        router.navigate(screen = Screen.Home)
        router.navigate(screen = Screen.TaskList)
        router.navigate(screen = Screen.Error(error = error))

        setUI(compositionValues = listOf(LocalAppState provides appState)) { ErrorScreen(error = error) }

        onNodeWithTag(testTag = "action_bar").assertIsDisplayed()
        onNodeWithTag(testTag = "action_back").assertIsDisplayed().performClick()
        waitForIdle()

        return router.backStack.toList()
    }

    /** Verifies that leaving a feedback screen the user opened on purpose simply pops it off the back stack. */
    @Test
    fun backFromFeedback() = runUITest {
        assertEquals(expected = listOf(Screen.Home, Screen.TaskList), actual = navigateBack(error = false, appState = AppState.AUTHENTICATED))
    }

    /** Verifies that leaving an error while loading restarts from the splash screen alone. */
    @Test
    fun backFromErrorWhileLoading() = runUITest {
        assertEquals(expected = listOf(Screen.Splash), actual = navigateBack(error = true, appState = AppState.LOADING))
    }

    /** Verifies that leaving an error while unauthenticated restarts from the login screen alone when the home screen is disabled. */
    @Test
    fun backFromErrorWhileUnauthenticated() = runUITest {
        ClientFlags.set { it.copy(home = false) }

        assertEquals(expected = listOf(Screen.Login), actual = navigateBack(error = true, appState = AppState.UNAUTHENTICATED))
    }

    /** Verifies that leaving an error while authenticated restarts from the user profile alone when the home screen is disabled. */
    @Test
    fun backFromErrorWhileAuthenticated() = runUITest {
        ClientFlags.set { it.copy(home = false) }

        assertEquals(expected = listOf(Screen.UserProfile), actual = navigateBack(error = true, appState = AppState.AUTHENTICATED))
    }

    /** Verifies that leaving an error restarts from the home screen alone when it is enabled. */
    @Test
    fun backFromErrorToHome() = runUITest {
        assertEquals(expected = listOf(Screen.Home), actual = navigateBack(error = true, appState = AppState.AUTHENTICATED))
    }
}
