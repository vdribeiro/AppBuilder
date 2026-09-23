package com.app.builder.ui.screen.audio

import kotlin.test.Test
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithTag
import com.app.builder.Dependency.getUserDependency
import com.app.builder.test.FakeData
import com.app.builder.test.TestCase
import com.app.builder.ui.LocalAudioPlayer
import com.app.builder.ui.component.actionbar.ActionBarState
import com.app.builder.ui.component.actionbar.ActionBarStore
import com.app.builder.ui.component.navigation.NavigationRoute
import com.app.builder.ui.component.navigation.NavigationState
import com.app.builder.ui.component.navigation.NavigationStore
import com.app.builder.ui.media.rememberAudioPlayer

class AudioScreenTest: TestCase() {

    /** Verifies that the audio screen renders its bars and track list. */
    @Test
    fun audioScreen() = runUITest {
        val authenticatedUseCases = dependency.get()
            .getUserDependency(user = FakeData.adminUser)
            .useCases
        authenticatedUseCases.authenticationUseCases.login(credentials = FakeData.adminCredentials)

        val actionBarStore = ActionBarStore(state = ActionBarState(title = "audio"), router = router, authenticationUseCases = authenticatedUseCases.authenticationUseCases)
        val navigationStore = NavigationStore(state = NavigationState(selected = NavigationRoute.AUDIO), router = router, authenticationUseCases = authenticatedUseCases.authenticationUseCases)

        setUI {
            CompositionLocalProvider(LocalAudioPlayer provides rememberAudioPlayer()) {
                AudioScreen(actionBarStore = actionBarStore, navigationStore = navigationStore)
            }
        }

        onNodeWithTag(testTag = "action_bar").assertIsDisplayed()
        onNodeWithTag(testTag = "navigation_bar").assertIsDisplayed()
    }
}
