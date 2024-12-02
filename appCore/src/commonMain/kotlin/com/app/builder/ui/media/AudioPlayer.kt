package com.app.builder.ui.media

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.app.builder.core.media.AudioPlayer
import com.app.builder.ui.lifecycle.Register

/**
 * Creates and remembers an [AudioPlayer] with a registered lifecycle.
 *
 * @return [AudioPlayer] instance.
 */
@Composable
fun rememberAudioPlayer(): AudioPlayer = remember { AudioPlayer.create() }.also { RegisterAudioPlayerLifecycle(audioPlayer = it) }

/**
 * Registers a lifecycle callback for audio playback to pause when the app is in the background and resume when the app is in the foreground.
 *
 * @param audioPlayer to register lifecycle.
 */
@Composable
private fun RegisterAudioPlayerLifecycle(audioPlayer: AudioPlayer) {
    var state by remember { mutableStateOf(value = audioPlayer.state.value) }
    Register(
        audioPlayer,
        onBackground = {
            state = audioPlayer.state.value
            audioPlayer.pause()
        },
        onForeground = {
            if (state == AudioPlayer.State.Playing) audioPlayer.play()
        },
        onDispose = {
            audioPlayer.dispose()
        }
    )
}
