package com.app.builder.ui.navigation.userprofile

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextReplacement
import com.app.builder.core.config.ClientFlags
import com.app.builder.test.FakeData
import com.app.builder.test.TestCase
import com.app.builder.ui.navigation.Screen

class ChangePasswordScreenTest: TestCase() {

    /** Verifies that the change password button stays disabled until every password is filled and the confirmation matches the new password, then submits on click and leaves the screen. */
    @Test
    fun changePassword() = runUITest {
        val authenticationUseCases = dependency.get()
            .userGraph
            .useCases
            .authenticationUseCases
        authenticationUseCases.login(credentials = FakeData.adminCredentials)
        router.navigate(screen = Screen.UserProfile)
        router.navigate(screen = Screen.ChangePassword)

        val store = ChangePasswordScreenStore(state = ChangePasswordScreenState(), router = router, authenticationUseCases = authenticationUseCases)

        setUI { ChangePasswordScreen(store = store) }

        waitUntil { store.state.userUuid == FakeData.adminUser.uuid }
        onNodeWithTag(testTag = "current_password").assertIsDisplayed()
        onNodeWithTag(testTag = "new_password").assertIsDisplayed()
        onNodeWithTag(testTag = "confirm_password").assertIsDisplayed()
        onNodeWithTag(testTag = "change_password").assertIsDisplayed().assertIsNotEnabled()

        onNodeWithTag(testTag = "current_password").performTextInput(text = FakeData.adminCredentials.password)
        onNodeWithTag(testTag = "new_password").performTextInput(text = "new_password")
        onNodeWithTag(testTag = "change_password").assertIsNotEnabled()

        onNodeWithTag(testTag = "confirm_password").performTextInput(text = "another_password")
        onNodeWithTag(testTag = "change_password").assertIsNotEnabled()

        onNodeWithTag(testTag = "confirm_password").performTextReplacement(text = "new_password")
        onNodeWithTag(testTag = "change_password").assertIsEnabled().performClick()

        waitUntil(timeoutMillis = 10_000) { router.backStack.toList() == listOf(Screen.UserProfile) }
        assertNull(actual = store.state.message)
    }

    /** Verifies that each password is masked until its own show button is clicked. */
    @Test
    fun showPassword() = runUITest {
        val authenticationUseCases = dependency.get()
            .userGraph
            .useCases
            .authenticationUseCases

        setUI { ChangePasswordScreen(store = ChangePasswordScreenStore(state = ChangePasswordScreenState(), router = router, authenticationUseCases = authenticationUseCases)) }

        onNodeWithTag(testTag = "current_password").performTextInput(text = "current")
        onNodeWithTag(testTag = "new_password").performTextInput(text = "new")
        onAllNodesWithContentDescription(label = "show_password").assertCountEquals(expectedSize = 3)

        onAllNodesWithContentDescription(label = "show_password")[1].performClick()
        onNodeWithTag(testTag = "current_password").assertTextContains(value = "•••••••")
        onNodeWithTag(testTag = "new_password").assertTextContains(value = "new")
        onAllNodesWithContentDescription(label = "show_password").assertCountEquals(expectedSize = 2)
        onNodeWithContentDescription(label = "hide_password").assertIsDisplayed()
    }

    /** Verifies that a wrong current password keeps the screen open and shows a message that can be dismissed. */
    @Test
    fun wrongCurrentPassword() = runUITest {
        val authenticationUseCases = dependency.get()
            .userGraph
            .useCases
            .authenticationUseCases
        authenticationUseCases.login(credentials = FakeData.adminCredentials)
        router.navigate(screen = Screen.UserProfile)
        router.navigate(screen = Screen.ChangePassword)

        val store = ChangePasswordScreenStore(state = ChangePasswordScreenState(), router = router, authenticationUseCases = authenticationUseCases)

        setUI { ChangePasswordScreen(store = store) }

        waitUntil { store.state.userUuid == FakeData.adminUser.uuid }
        onNodeWithTag(testTag = "current_password").performTextInput(text = "wrong_password")
        onNodeWithTag(testTag = "new_password").performTextInput(text = "new_password")
        onNodeWithTag(testTag = "confirm_password").performTextInput(text = "new_password")
        onNodeWithTag(testTag = "change_password").performClick()

        waitUntil(timeoutMillis = 10_000) { store.state.message == "unable_to_change_password" }
        assertFalse(actual = store.state.loading)
        assertEquals(expected = listOf(Screen.UserProfile, Screen.ChangePassword), actual = router.backStack.toList())

        store.send(action = ChangePasswordScreenAction.DismissMessage)
        assertNull(actual = store.state.message)
    }

    /** Verifies that a password change without a connection keeps the screen open and shows a message, and that the back button leaves the screen. */
    @Test
    fun failedPasswordChange() = runUITest {
        val authenticationUseCases = dependency.get()
            .userGraph
            .useCases
            .authenticationUseCases
        authenticationUseCases.login(credentials = FakeData.adminCredentials)
        router.navigate(screen = Screen.UserProfile)
        router.navigate(screen = Screen.ChangePassword)

        val store = ChangePasswordScreenStore(state = ChangePasswordScreenState(), router = router, authenticationUseCases = authenticationUseCases)

        setUI { ChangePasswordScreen(store = store) }

        waitUntil { store.state.userUuid == FakeData.adminUser.uuid }
        ClientFlags.set { it.copy(http = false) }
        onNodeWithTag(testTag = "current_password").performTextInput(text = FakeData.adminCredentials.password)
        onNodeWithTag(testTag = "new_password").performTextInput(text = "new_password")
        onNodeWithTag(testTag = "confirm_password").performTextInput(text = "new_password")
        onNodeWithTag(testTag = "change_password").performClick()

        waitUntil(timeoutMillis = 10_000) { store.state.message == "unable_to_change_password" }
        assertFalse(actual = store.state.loading)
        assertTrue(actual = store.state.currentPassword.isNotEmpty())

        onNodeWithTag(testTag = "action_back").performClick()
        waitForIdle()
        assertEquals(expected = listOf(Screen.UserProfile), actual = router.backStack.toList())
    }
}
