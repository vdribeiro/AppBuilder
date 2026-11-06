package com.app.builder.ui.navigation.home

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.app.builder.test.FakeData
import com.app.builder.test.TestCase
import com.app.builder.ui.component.session.SessionState
import com.app.builder.ui.component.session.SessionStore
import com.app.builder.ui.navigation.Screen

class HomeScreenTest: TestCase() {

    /** Verifies that a guest landing on the home screen is offered the login button, which opens the login screen. */
    @Test
    fun guestHome() = runUITest {
        val authenticationUseCases = dependency.get()
            .userGraph
            .useCases
            .authenticationUseCases

        val sessionStore = SessionStore(state = SessionState(), authenticationUseCases = authenticationUseCases)

        setUI { HomeScreen(sessionStore = sessionStore) }

        onNodeWithTag(testTag = "action_bar").assertIsDisplayed()
        onNodeWithText(text = "Home").assertIsDisplayed()

        assertTrue(actual = router.backStack.isEmpty())
        onNodeWithTag(testTag = "action_login").assertIsDisplayed().performClick()
        assertEquals(expected = listOf(Screen.Login), actual = router.backStack.toList())
    }

    /** Verifies that a logged in user is not offered the login button, and that their avatar opens the user profile. */
    @Test
    fun authenticatedHome() = runUITest {
        val authenticationUseCases = dependency.get()
            .userGraph
            .useCases
            .authenticationUseCases
        authenticationUseCases.login(credentials = FakeData.adminCredentials)

        val sessionStore = SessionStore(state = SessionState(), authenticationUseCases = authenticationUseCases)

        setUI { HomeScreen(sessionStore = sessionStore) }

        waitUntil { sessionStore.state.user != null }
        onNodeWithTag(testTag = "action_login").assertDoesNotExist()

        onNodeWithTag(testTag = "action_avatar").assertIsDisplayed().performClick()
        assertEquals(expected = listOf(Screen.UserProfile), actual = router.backStack.toList())
    }
}
