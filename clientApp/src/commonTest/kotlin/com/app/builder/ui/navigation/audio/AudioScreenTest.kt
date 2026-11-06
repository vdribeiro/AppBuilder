package com.app.builder.ui.navigation.audio

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
import com.app.builder.ui.component.session.SessionState
import com.app.builder.ui.component.session.SessionStore

/** Reports itself as available so the screen renders its controls, without reaching any platform media API. */
private class FakeAudioPlayer: AudioPlayer() {
    override val available: Boolean = true
}

class AudioScreenTest: TestCase() {

    /** Verifies that the audio screen renders its bars and track list, and that its transport controls follow the player's state. */
    @Test
    fun audioScreen() = runUITest {
        val authenticatedUseCases = dependency.get()
            .getUserDependency(account = FakeData.adminAccount)
            .useCases
        authenticatedUseCases.authenticationUseCases.login(credentials = FakeData.adminCredentials)

        val sessionStore = SessionStore(state = SessionState(), authenticationUseCases = authenticatedUseCases.authenticationUseCases)
        val audioPlayer = FakeAudioPlayer()

        setUI {
            CompositionLocalProvider(LocalAudioPlayer provides audioPlayer) {
                AudioScreen(sessionStore = sessionStore)
            }
        }

        onNodeWithTag(testTag = "action_bar").assertIsDisplayed()
        onNodeWithTag(testTag = "navigation_bar").assertIsDisplayed()
        onNodeWithText(text = "Ping").assertIsDisplayed()

        // Idle: nothing is playing yet, so only Play is offered.
        onNodeWithText(text = "Play").assertIsEnabled()
        onNodeWithText(text = "Pause").assertIsNotEnabled()
        onNodeWithText(text = "Stop").assertIsNotEnabled()

        // Setting a playlist leaves the player idle, so the controls do not move.
        onNodeWithText(text = "Set playlist").performClick()
        waitForIdle()

        onNodeWithText(text = "Play").assertIsEnabled()
        onNodeWithText(text = "Pause").assertIsNotEnabled()
        onNodeWithText(text = "Stop").assertIsNotEnabled()

        // Playing: Play gives way to Pause and Stop.
        onNodeWithText(text = "Play").performClick()
        waitUntil { audioPlayer.state.value == AudioPlayer.State.Playing }

        onNodeWithText(text = "Play").assertIsNotEnabled()
        onNodeWithText(text = "Pause").assertIsEnabled()
        onNodeWithText(text = "Stop").assertIsEnabled()

        // Paused: playback can resume or be stopped.
        onNodeWithText(text = "Pause").performClick()
        waitUntil { audioPlayer.state.value == AudioPlayer.State.Paused }

        onNodeWithText(text = "Play").assertIsEnabled()
        onNodeWithText(text = "Pause").assertIsNotEnabled()
        onNodeWithText(text = "Stop").assertIsEnabled()

        // Stopped: back to the idle controls.
        onNodeWithText(text = "Stop").performClick()
        waitUntil { audioPlayer.state.value == AudioPlayer.State.Idle }

        onNodeWithText(text = "Play").assertIsEnabled()
        onNodeWithText(text = "Pause").assertIsNotEnabled()
        onNodeWithText(text = "Stop").assertIsNotEnabled()
    }
}
