package com.app.builder.ui.screen.audio

import kotlin.test.Test
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.app.builder.Dependency.getUserDependency
import com.app.builder.core.media.AudioPlayer
import com.app.builder.test.FakeData
import com.app.builder.test.TestCase
import com.app.builder.ui.LocalAudioPlayer
import com.app.builder.ui.component.actionbar.ActionBarState
import com.app.builder.ui.component.actionbar.ActionBarStore
import com.app.builder.ui.component.navigation.NavigationRoute
import com.app.builder.ui.component.navigation.NavigationState
import com.app.builder.ui.component.navigation.NavigationStore

/** Reports itself as available so the screen renders its controls, without reaching any platform media API. */
private class FakeAudioPlayer: AudioPlayer() {
    override val available: Boolean = true
}

class AudioScreenTest: TestCase() {

    /** Verifies that the audio screen renders its bars and track list, and that playback can only start once a playlist has been set. */
    @Test
    fun audioScreen() = runUITest {
        val authenticatedUseCases = dependency.get()
            .getUserDependency(user = FakeData.adminUser)
            .useCases
        authenticatedUseCases.authenticationUseCases.login(credentials = FakeData.adminCredentials)

        val actionBarStore = ActionBarStore(state = ActionBarState(title = "audio"), router = router, authenticationUseCases = authenticatedUseCases.authenticationUseCases)
        val navigationStore = NavigationStore(state = NavigationState(selected = NavigationRoute.AUDIO), router = router, authenticationUseCases = authenticatedUseCases.authenticationUseCases)

        setUI {
            CompositionLocalProvider(LocalAudioPlayer provides FakeAudioPlayer()) {
                AudioScreen(actionBarStore = actionBarStore, navigationStore = navigationStore)
            }
        }

        onNodeWithTag(testTag = "action_bar").assertIsDisplayed()
        onNodeWithTag(testTag = "navigation_bar").assertIsDisplayed()
        onNodeWithText(text = "Ping").assertIsDisplayed()

        onNodeWithText(text = "Play").assertIsNotEnabled()

        onNodeWithText(text = "Set playlist").performClick()
        waitForIdle()

        onNodeWithText(text = "Play").assertIsEnabled()
    }
}
