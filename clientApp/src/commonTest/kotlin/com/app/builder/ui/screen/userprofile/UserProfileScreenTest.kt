package com.app.builder.ui.screen.userprofile

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.flow.firstOrNull
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.app.builder.test.FakeData
import com.app.builder.test.TestCase
import com.app.builder.ui.component.navigation.NavigationState
import com.app.builder.ui.component.navigation.NavigationStore
import com.app.builder.ui.navigation.Screen

class UserProfileScreenTest: TestCase() {

    /** Verifies that a guest only gets the about section, and that sending feedback opens the feedback screen. */
    @Test
    fun guestProfile() = runUITest {
        val authenticationUseCases = dependency.get()
            .userGraph
            .useCases
            .authenticationUseCases

        val navigationStore = NavigationStore(state = NavigationState(), router = router, authenticationUseCases = authenticationUseCases)
        val store = UserProfileScreenStore(state = UserProfileScreenState(guest = false), authenticationUseCases = authenticationUseCases)

        setUI { UserProfileScreen(navigationStore = navigationStore, store = store) }

        waitUntil { store.state.guest }
        assertNull(actual = store.state.name)

        onNodeWithText(text = "about").assertIsDisplayed()
        onNodeWithText(text = "account").assertDoesNotExist()
        onNodeWithText(text = "logout").assertDoesNotExist()
        onNodeWithText(text = "device").assertDoesNotExist()

        onNodeWithText(text = "send_feedback").performScrollTo().performClick()
        assertEquals(expected = listOf(Screen.Error(error = false)), actual = router.backStack.toList())
    }

    /** Verifies that a logged in user sees their name and account settings, and that logging out turns the profile back into a guest one. */
    @Test
    fun logout() = runUITest {
        val authenticationUseCases = dependency.get()
            .userGraph
            .useCases
            .authenticationUseCases
        authenticationUseCases.login(credentials = FakeData.adminCredentials)

        val navigationStore = NavigationStore(state = NavigationState(), router = router, authenticationUseCases = authenticationUseCases)
        val store = UserProfileScreenStore(state = UserProfileScreenState(), authenticationUseCases = authenticationUseCases)

        setUI { UserProfileScreen(navigationStore = navigationStore, store = store) }

        waitUntil { !store.state.guest }
        assertEquals(expected = FakeData.adminUser.name, actual = store.state.name)

        onNodeWithText(text = FakeData.adminUser.name).assertIsDisplayed()
        onNodeWithText(text = "account").assertIsDisplayed()
        onNodeWithText(text = "logout").performScrollTo().performClick()

        waitUntil { store.state.guest }
        assertNull(actual = store.state.name)
        assertNull(actual = authenticationUseCases.observeCurrentUser().firstOrNull())
        onNodeWithText(text = "account").assertDoesNotExist()
        assertTrue(actual = router.backStack.isEmpty())
    }
}
