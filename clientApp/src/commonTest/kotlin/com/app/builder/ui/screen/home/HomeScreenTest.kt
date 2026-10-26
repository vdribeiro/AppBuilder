package com.app.builder.ui.screen.home

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.app.builder.test.FakeData
import com.app.builder.test.TestCase
import com.app.builder.ui.component.actionbar.ActionBarState
import com.app.builder.ui.component.actionbar.ActionBarStore
import com.app.builder.ui.component.navigation.NavigationRoute
import com.app.builder.ui.component.navigation.NavigationState
import com.app.builder.ui.component.navigation.NavigationStore
import com.app.builder.ui.navigation.Screen

class HomeScreenTest: TestCase() {

    /** Verifies that a guest landing on the home screen is offered the login button, which opens the login screen. */
    @Test
    fun guestHome() = runUITest {
        val authenticationUseCases = dependency.get()
            .userGraph
            .useCases
            .authenticationUseCases

        val actionBarStore = ActionBarStore(state = ActionBarState(), router = router, authenticationUseCases = authenticationUseCases)
        val navigationStore = NavigationStore(state = NavigationState(selected = NavigationRoute.HOME), router = router, authenticationUseCases = authenticationUseCases)

        setUI { HomeScreen(actionBarStore = actionBarStore, navigationStore = navigationStore) }

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

        val actionBarStore = ActionBarStore(state = ActionBarState(), router = router, authenticationUseCases = authenticationUseCases)
        val navigationStore = NavigationStore(state = NavigationState(selected = NavigationRoute.HOME), router = router, authenticationUseCases = authenticationUseCases)

        setUI { HomeScreen(actionBarStore = actionBarStore, navigationStore = navigationStore) }

        waitUntil { !actionBarStore.state.showLogin }
        onNodeWithTag(testTag = "action_login").assertDoesNotExist()

        onNodeWithTag(testTag = "action_avatar").assertIsDisplayed().performClick()
        assertEquals(expected = listOf(Screen.UserProfile), actual = router.backStack.toList())
    }
}
