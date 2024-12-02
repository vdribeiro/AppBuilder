package com.app.builder.ui.media

import kotlin.test.Test
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import com.app.builder.core.media.AudioPlayer
import com.app.builder.test.TestCase

class AudioPlayerTest: TestCase() {

    /** Verifies that [RegisterAudioPlayerLifecycle] does not pause playback merely from mounting. */
    @Test
    fun audioPlayerLifecycleDoesNotPauseOnStart() = runUITest {
        var audioPlayer: AudioPlayer? = null

        setUI { audioPlayer = rememberAudioPlayer() }

        assertNotEquals(illegal = AudioPlayer.State.Paused, actual = audioPlayer?.state?.value)
    }

    /** Verifies that [rememberAudioPlayer] provides an audio player. */
    @Test
    fun rememberAudioPlayerProvidesAPlayer() = runUITest {
        var audioPlayer: AudioPlayer? = null

        setUI { audioPlayer = rememberAudioPlayer() }

        assertNotNull(actual = audioPlayer)
    }

    /** Verifies that [rememberAudioPlayer] returns the same instance across recompositions. */
    @Test
    fun rememberAudioPlayerIsStableAcrossRecompositions() = runUITest {
        val players = mutableListOf<AudioPlayer>()

        setUI {
            val counter = remember { mutableIntStateOf(value = 0) }
            players.add(element = rememberAudioPlayer())
            LaunchedEffect(key1 = Unit) { counter.value = 1 }
        }
        waitForIdle()

        assertTrue(actual = players.isNotEmpty())
        assertTrue(actual = players.all { it === players.first() })
    }
}
