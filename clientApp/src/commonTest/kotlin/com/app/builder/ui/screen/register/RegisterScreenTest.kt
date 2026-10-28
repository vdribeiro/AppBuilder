package com.app.builder.ui.screen.register

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import app.cash.sqldelight.async.coroutines.awaitAsOneOrNull
import com.app.builder.Dependency.getUserDependency
import com.app.builder.domain.EntityType
import com.app.builder.domain.Permission
import com.app.builder.domain.gateway.authentication.toUser
import com.app.builder.test.FakeData
import com.app.builder.test.TestCase
import com.app.builder.ui.component.actionbar.ActionBarState
import com.app.builder.ui.component.actionbar.ActionBarStore
import com.app.builder.ui.component.navigation.NavigationRoute
import com.app.builder.ui.component.navigation.NavigationState
import com.app.builder.ui.component.navigation.NavigationStore

class RegisterScreenTest: TestCase() {

    /** Verifies that filling the registration form, granting a permission, and submitting registers a user holding that permission. */
    @Test
    fun registerScreen() = runUITest {
        val authenticatedUseCases = dependency.get()
            .getUserDependency(user = FakeData.adminUser)
            .useCases
        authenticatedUseCases.authenticationUseCases.login(credentials = FakeData.adminCredentials)

        val navigationStore = NavigationStore(state = NavigationState(selected = NavigationRoute.USER), router = router, authenticationUseCases = authenticatedUseCases.authenticationUseCases)
        val registrationScreenStore = RegistrationScreenStore(state = RegistrationScreenState(), authenticationUseCases = authenticatedUseCases.authenticationUseCases)

        val actionBarStore = ActionBarStore(state = ActionBarState(), router = router, authenticationUseCases = authenticatedUseCases.authenticationUseCases)

        setUI { RegisterScreen(actionBarStore = actionBarStore, navigationStore = navigationStore, store = registrationScreenStore) }

        onNodeWithTag(testTag = "navigation_bar").assertIsDisplayed()
        assertTrue(actual = navigationStore.state.items.first { it.text == "user" }.selected)

        onNodeWithTag(testTag = "register").assertIsDisplayed()
        onNodeWithTag(testTag = "register").assertIsNotEnabled()

        onNodeWithTag(testTag = "name").performTextInput(text = FakeData.adminUser.name)
        onNodeWithTag(testTag = "username").performTextInput(text = USERNAME)
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

        val registered = dependency.get()
            .database
            .userQueries
            .getUserByUsername(username = USERNAME)
            .awaitAsOneOrNull()
            ?.toUser()
        assertNotEquals(illegal = FakeData.adminUser.uuid, actual = registered?.uuid)
        assertEquals(expected = FakeData.adminUser.name, actual = registered?.name)
        assertEquals(expected = mapOf(EntityType.USER to Permission.WRITE), actual = registered?.permissions)
    }

    companion object {
        /** Username of the account registered through the form, distinct from the logged in admin's. */
        private const val USERNAME = "registered"
    }
}
