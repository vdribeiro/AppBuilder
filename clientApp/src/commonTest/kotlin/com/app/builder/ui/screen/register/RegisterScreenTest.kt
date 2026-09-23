package com.app.builder.ui.screen.register

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import com.app.builder.Dependency.getUserDependency
import com.app.builder.domain.EntityType
import com.app.builder.test.FakeData
import com.app.builder.test.TestCase
import com.app.builder.ui.component.actionbar.ActionBarState
import com.app.builder.ui.component.actionbar.ActionBarStore
import com.app.builder.ui.component.navigation.NavigationRoute
import com.app.builder.ui.component.navigation.NavigationState
import com.app.builder.ui.component.navigation.NavigationStore

class RegisterScreenTest: TestCase() {

    /** Verifies that filling the registration form, granting a permission, and submitting completes registration and clears the back stack. */
    @Test
    fun registerScreen() = runUITest {
        val authenticatedUseCases = dependency.get()
            .getUserDependency(user = FakeData.adminUser)
            .useCases
        authenticatedUseCases.authenticationUseCases.login(credentials = FakeData.adminCredentials)

        val navigationStore = NavigationStore(state = NavigationState(selected = NavigationRoute.USER), router = router, authenticationUseCases = authenticatedUseCases.authenticationUseCases)
        val registrationScreenStore = RegistrationScreenStore(state = RegistrationScreenState(), authenticationUseCases = authenticatedUseCases.authenticationUseCases)

        val actionBarStore = ActionBarStore(state = ActionBarState(title = "registration"), router = router, authenticationUseCases = authenticatedUseCases.authenticationUseCases)

        setUI { RegisterScreen(actionBarStore = actionBarStore, navigationStore = navigationStore, store = registrationScreenStore) }

        onNodeWithTag(testTag = "navigation_bar").assertIsDisplayed()
        assertTrue(actual = navigationStore.state.items.first { it.text == "user" }.selected)

        onNodeWithTag(testTag = "register").assertIsDisplayed()
        onNodeWithTag(testTag = "register").assertIsNotEnabled()

        onNodeWithTag(testTag = "name").performTextInput(text = FakeData.adminUser.name)
        onNodeWithTag(testTag = "username").performTextInput(text = FakeData.adminCredentials.username)
        onNodeWithTag(testTag = "password").performTextInput(text = FakeData.adminCredentials.password)
        onNodeWithTag(testTag = "register").assertIsEnabled()

        val entityId = EntityType.USER.name
        onNodeWithTag(testTag = "${entityId}_write").performScrollTo().performClick()
        waitForIdle()

        val permission = registrationScreenStore.state.permissions.first { it.id == entityId }
        assertTrue(actual = permission.read)
        assertTrue(actual = permission.write)

        onNodeWithTag(testTag = "register").performClick()
        waitForIdle()

        waitUntil { !registrationScreenStore.state.loading }
        assertFalse(actual = registrationScreenStore.state.loading)
        assertTrue(actual = router.backStack.isEmpty())
    }
}
