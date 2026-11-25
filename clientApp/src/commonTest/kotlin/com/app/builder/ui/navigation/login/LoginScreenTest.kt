package com.app.builder.ui.navigation.login

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.flow.firstOrNull
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import com.app.builder.core.config.ClientFlags
import com.app.builder.test.FakeData
import com.app.builder.test.TestCase
import com.app.builder.ui.navigation.ScreenMessage

class LoginScreenTest: TestCase() {

    override suspend fun beforeTest() {
        super.beforeTest()
        ClientFlags.set { it.copy(http = true) }
    }

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

        onNodeWithText(text = "login_app_name").assertIsDisplayed()

        onNodeWithText(text = "login_username").assertIsDisplayed()
        onNodeWithTag(testTag = "username").assertIsDisplayed()

        onNodeWithText(text = "login_password").assertIsDisplayed()
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

        waitUntil(timeoutMillis = 10_000) { !store.state.loading }
        assertFalse(actual = store.state.loading)
        assertEquals(expected = FakeData.adminUser.uuid, actual = authenticationUseCases.observeCurrentUser().firstOrNull()?.uuid)
    }

    /** Verifies that a refused login keeps the user logged out and shows why, in a message that can be dismissed. */
    @Test
    fun refusedLogin() = runUITest {
        val authenticationUseCases = dependency.get()
            .userGraph
            .useCases
            .authenticationUseCases
        val store = LoginScreenStore(state = LoginScreenState(), authenticationUseCases = authenticationUseCases)

        setUI { LoginScreen(store = store) }

        onNodeWithTag(testTag = "username").performTextInput(text = FakeData.adminCredentials.username)
        onNodeWithTag(testTag = "password").performTextInput(text = "wrong_password")
        onNodeWithTag(testTag = "login").performClick()

        waitUntil(timeoutMillis = 10_000) { store.state.message == ScreenMessage(key = "login_wrong_credentials") }
        assertFalse(actual = store.state.loading)
        onNodeWithText(text = "login_wrong_credentials").assertIsDisplayed()
        assertNull(actual = authenticationUseCases.observeCurrentUser().firstOrNull())

        store.send(action = LoginScreenAction.DismissMessage)
        waitUntil { store.state.message == null }
    }

    /** Verifies that the password is masked until its show button is clicked, and masked again once its hide button is clicked. */
    @Test
    fun showAndHidePassword() = runUITest {
        val authenticationUseCases = dependency.get()
            .userGraph
            .useCases
            .authenticationUseCases

        setUI { LoginScreen(store = LoginScreenStore(state = LoginScreenState(), authenticationUseCases = authenticationUseCases)) }

        onNodeWithTag(testTag = "password").performTextInput(text = "secret")
        onNodeWithTag(testTag = "password").assertTextEquals("••••••")

        onNodeWithContentDescription(label = "text_field_show_password").performClick()
        onNodeWithTag(testTag = "password").assertTextEquals("secret")

        onNodeWithContentDescription(label = "text_field_hide_password").performClick()
        onNodeWithTag(testTag = "password").assertTextEquals("••••••")
        onNodeWithContentDescription(label = "text_field_show_password").assertIsDisplayed()
    }
}
