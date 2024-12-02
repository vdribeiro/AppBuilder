package com.app.builder.ui.screen.push

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import com.app.builder.Dependency.getUserDependency
import com.app.builder.test.FakeData
import com.app.builder.test.TestCase
import com.app.builder.ui.component.button.PushPayloadType
import com.app.builder.ui.component.navigation.NavigationRoute
import com.app.builder.ui.component.navigation.NavigationState
import com.app.builder.ui.component.navigation.NavigationStore
import com.app.builder.ui.component.useravatar.UserAvatarState
import com.app.builder.ui.component.useravatar.UserAvatarStore

class PushScreenTest: TestCase() {

    /** Verifies sending push notifications as a broadcast, to a selected user, and for flags/configs/reset payload types. */
    @Test
    fun pushScreen() = runUITest {
        val authenticatedUseCases = dependency.get()
            .getUserDependency(user = FakeData.adminUser)
            .useCases
        authenticatedUseCases.authenticationUseCases.login(credentials = FakeData.adminCredentials)

        val navigationStore = NavigationStore(state = NavigationState(selected = NavigationRoute.NOTIFICATION), router = router, authenticationUseCases = authenticatedUseCases.authenticationUseCases)
        val pushScreenStore = PushScreenStore(state = PushScreenState(), pushUseCases = authenticatedUseCases.pushUseCases, userUseCases = authenticatedUseCases.userUseCases)

        val userAvatarStore = UserAvatarStore(state = UserAvatarState(), authenticationUseCases = authenticatedUseCases.authenticationUseCases)

        setUI { PushScreen(navigationStore = navigationStore, userAvatarStore = userAvatarStore, store = pushScreenStore) }

        onNodeWithTag(testTag = "navigation_bar").assertIsDisplayed()
        assertTrue(actual = navigationStore.state.items.first { it.text == "push" }.selected)
        onNodeWithText(text = "push_field_type").assertIsDisplayed()
        onNodeWithText(text = "push_field_broadcast").assertIsDisplayed()
        onNodeWithText(text = "push_field_title").assertIsDisplayed()
        onNodeWithText(text = "push_field_description").assertIsDisplayed()
        onNodeWithText(text = "push_send").assertIsDisplayed()
        assertEquals(expected = listOf(FakeData.adminUser.name), actual = pushScreenStore.state.users.map { it.name })

        pushScreenStore.send(action = PushScreenAction.UpdateTitle(value = "Title"))
        pushScreenStore.send(action = PushScreenAction.UpdateDescription(value = "Description"))
        pushScreenStore.send(action = PushScreenAction.Send)
        waitForIdle()

        onNodeWithText(text = "push_sent").assertIsDisplayed()
        assertFalse(actual = pushScreenStore.state.sending)
        assertEquals(expected = 1, actual = pushScreenStore.state.sent)

        pushScreenStore.send(action = PushScreenAction.ToggleBroadcast(value = false))
        waitForIdle()

        onNodeWithText(text = "users").assertIsDisplayed()
        val user = pushScreenStore.state.users.first()
        pushScreenStore.send(action = PushScreenAction.ToggleUser(user = user))
        waitForIdle()
        assertEquals(expected = listOf(user), actual = pushScreenStore.state.selectedUsers)

        pushScreenStore.send(action = PushScreenAction.Send)
        waitForIdle()
        assertEquals(expected = 1, actual = pushScreenStore.state.sent)

        pushScreenStore.send(action = PushScreenAction.SelectType(type = PushPayloadType.FLAGS))
        pushScreenStore.send(action = PushScreenAction.ToggleBroadcast(value = true))
        pushScreenStore.send(action = PushScreenAction.Send)
        waitForIdle()
        assertEquals(expected = 1, actual = pushScreenStore.state.sent)

        pushScreenStore.send(action = PushScreenAction.SelectType(type = PushPayloadType.CONFIGS))
        pushScreenStore.send(action = PushScreenAction.Send)
        waitForIdle()
        assertEquals(expected = 1, actual = pushScreenStore.state.sent)

        pushScreenStore.send(action = PushScreenAction.SelectType(type = PushPayloadType.RESET))
        pushScreenStore.send(action = PushScreenAction.Send)
        waitForIdle()
        assertEquals(expected = 1, actual = pushScreenStore.state.sent)
    }
}
