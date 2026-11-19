package com.app.builder.ui.navigation.userprofile

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.flow.firstOrNull
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.app.builder.core.config.ClientFlags
import com.app.builder.test.FakeData
import com.app.builder.test.TestCase
import com.app.builder.ui.navigation.Screen

class UserProfileScreenTest: TestCase() {

    override suspend fun beforeTest() {
        super.beforeTest()
        ClientFlags.set { it.copy(http = true, help = true) }
    }

    /** Verifies that a guest only gets the about section without help, and that sending feedback opens the feedback screen. */
    @Test
    fun guestProfile() = runUITest {
        val authenticationUseCases = dependency.get()
            .userGraph
            .useCases
            .authenticationUseCases

        val store = UserProfileScreenStore(state = UserProfileScreenState(guest = false), authenticationUseCases = authenticationUseCases)

        setUI { UserProfileScreen(store = store) }

        waitUntil { store.state.guest }
        assertNull(actual = store.state.name)

        onNodeWithText(text = "about").assertIsDisplayed()
        onNodeWithText(text = "account").assertDoesNotExist()
        onNodeWithText(text = "logout").assertDoesNotExist()
        onNodeWithText(text = "device").assertDoesNotExist()
        onNodeWithText(text = "help").assertDoesNotExist()

        onNodeWithText(text = "send_feedback").performScrollTo().performClick()
        assertEquals(expected = listOf(Screen.Error(error = false)), actual = router.backStack.toList())
    }

    /** Verifies that help is hidden for a logged in user while the help client flag is disabled. */
    @Test
    fun helpHiddenWhenDisabled() = runUITest {
        val authenticationUseCases = dependency.get()
            .userGraph
            .useCases
            .authenticationUseCases
        authenticationUseCases.login(credentials = FakeData.adminCredentials)

        val store = UserProfileScreenStore(state = UserProfileScreenState(), authenticationUseCases = authenticationUseCases)

        setUI { UserProfileScreen(store = store) }

        waitUntil { !store.state.guest }
        onNodeWithText(text = "help").performScrollTo().assertIsDisplayed()

        ClientFlags.set { it.copy(help = false) }
        waitForIdle()

        onNodeWithText(text = "help").assertDoesNotExist()
        onNodeWithText(text = "send_feedback").performScrollTo().assertIsDisplayed()
    }

    /** Verifies that help opens the help screen for a logged in user. */
    @Test
    fun help() = runUITest {
        val authenticationUseCases = dependency.get()
            .userGraph
            .useCases
            .authenticationUseCases
        authenticationUseCases.login(credentials = FakeData.adminCredentials)

        val store = UserProfileScreenStore(state = UserProfileScreenState(), authenticationUseCases = authenticationUseCases)

        setUI { UserProfileScreen(store = store) }

        waitUntil { !store.state.guest }
        onNodeWithText(text = "help").performScrollTo().performClick()
        assertEquals(expected = listOf(Screen.Help), actual = router.backStack.toList())
    }

    /** Verifies that edit profile opens the edit profile screen for a logged in user. */
    @Test
    fun editProfile() = runUITest {
        val authenticationUseCases = dependency.get()
            .userGraph
            .useCases
            .authenticationUseCases
        authenticationUseCases.login(credentials = FakeData.adminCredentials)

        val store = UserProfileScreenStore(state = UserProfileScreenState(), authenticationUseCases = authenticationUseCases)

        setUI { UserProfileScreen(store = store) }

        waitUntil { !store.state.guest }
        onNodeWithText(text = "edit_profile").performScrollTo().performClick()
        assertEquals(expected = listOf(Screen.EditProfile), actual = router.backStack.toList())
    }

    /** Verifies that change password opens the change password screen for a logged in user. */
    @Test
    fun changePassword() = runUITest {
        val authenticationUseCases = dependency.get()
            .userGraph
            .useCases
            .authenticationUseCases
        authenticationUseCases.login(credentials = FakeData.adminCredentials)

        val store = UserProfileScreenStore(state = UserProfileScreenState(), authenticationUseCases = authenticationUseCases)

        setUI { UserProfileScreen(store = store) }

        waitUntil { !store.state.guest }
        onNodeWithText(text = "change_password").performScrollTo().performClick()
        assertEquals(expected = listOf(Screen.ChangePassword), actual = router.backStack.toList())
    }

    /** Verifies that a logged in user sees their name and account settings, and that logging out turns the profile back into a guest one. */
    @Test
    fun logout() = runUITest {
        val authenticationUseCases = dependency.get()
            .userGraph
            .useCases
            .authenticationUseCases
        authenticationUseCases.login(credentials = FakeData.adminCredentials)

        val store = UserProfileScreenStore(state = UserProfileScreenState(), authenticationUseCases = authenticationUseCases)

        setUI { UserProfileScreen(store = store) }

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
