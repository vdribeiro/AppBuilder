package com.app.builder.ui.screen.login

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.coroutines.flow.firstOrNull
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import com.app.builder.test.FakeData
import com.app.builder.test.TestCase

class LoginScreenTest: TestCase() {

    /** Verifies that the login button stays disabled until both username and password are filled, then submits on click and logs the user in. */
    @Test
    fun loginScreen() = runUITest {
        val authenticationUseCases = dependency.get()
            .userGraph
            .useCases
            .authenticationUseCases

        val store = LoginScreenStore(state = LoginScreenState(), authenticationUseCases = authenticationUseCases)

        setUI { LoginScreen(store = store) }

        assertTrue(actual = router.backStack.isEmpty())
        assertFalse(actual = store.state.loading)
        assertTrue(actual = store.state.username.isBlank())
        assertTrue(actual = store.state.password.isBlank())

        onNodeWithText(text = "app_name").assertIsDisplayed()

        onNodeWithText(text = "username").assertIsDisplayed()
        onNodeWithTag(testTag = "username").assertIsDisplayed()

        onNodeWithText(text = "password").assertIsDisplayed()
        onNodeWithTag(testTag = "password").assertIsDisplayed()

        onNodeWithTag(testTag = "login").assertIsDisplayed()
        onNodeWithTag(testTag = "login").assertIsNotEnabled()

        onNodeWithTag(testTag = "username").performTextInput(text = FakeData.adminCredentials.username)
        onNodeWithTag(testTag = "login").assertIsNotEnabled()
        assertTrue(actual = store.state.username.isNotEmpty())

        onNodeWithTag(testTag = "password").performTextInput(text = FakeData.adminCredentials.password)
        onNodeWithTag(testTag = "login").assertIsEnabled()
        assertTrue(actual = store.state.password.isNotEmpty())

        onNodeWithTag(testTag = "login").performClick()
        waitForIdle()

        waitUntil { !store.state.loading }
        assertFalse(actual = store.state.loading)
        assertEquals(expected = FakeData.adminUser.uuid, actual = authenticationUseCases.observeCurrentUser().firstOrNull()?.uuid)
    }
}
