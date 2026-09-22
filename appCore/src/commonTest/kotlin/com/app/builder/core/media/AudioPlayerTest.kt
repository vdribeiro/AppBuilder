package com.app.builder.core.media

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import com.app.builder.core.config.ClientFlags
import com.app.builder.test.TestCase

class AudioPlayerTest: TestCase() {

    /** A test fixture [AudioPlayer] that records every invoked action as an event. */
    private class RecordingAudioPlayer: AudioPlayer() {
        /** The events recorded so far by this test fixture. */
        val events = mutableListOf<String>()

        override val available: Boolean = true

        override fun platformSetPlaylist() {
            events.add(element = "setPlaylist")
        }

        override fun platformPlay() {
            events.add(element = "play")
        }

        override fun platformPause() {
            events.add(element = "pause")
        }

        override fun platformStop() {
            events.add(element = "stop")
        }

        override fun platformDispose() {
            events.add(element = "dispose")
        }

        /**
         * Exposes the protected [isPlaying] for assertions.
         *
         * @return `true` if the player is playing, `false` otherwise.
         */
        fun callIsPlaying(): Boolean = isPlaying()
    }

    /** Verifies that setting a playlist then playing produces the expected event sequence and state transitions. */
    @Test
    fun setPlaylistAndPlayTransitionsToPlaying() = runUnitTest {
        val audioPlayer = RecordingAudioPlayer()
        assertEquals(expected = AudioPlayer.State.Idle, actual = audioPlayer.state.value)

        audioPlayer.setPlaylist(playlist = listOf("a", "b"), loop = true, shuffle = false)
        audioPlayer.play()

        assertEquals(expected = listOf("stop", "setPlaylist", "play"), actual = audioPlayer.events)
        assertEquals(expected = AudioPlayer.State.Playing, actual = audioPlayer.state.value)
    }

    /** Verifies that pausing while playing transitions to paused. */
    @Test
    fun pauseTransitionsToPaused() = runUnitTest {
        val audioPlayer = RecordingAudioPlayer()
        audioPlayer.setPlaylist(playlist = listOf("a", "b"))
        audioPlayer.play()
        audioPlayer.events.clear()

        audioPlayer.pause()

        assertEquals(expected = listOf("pause"), actual = audioPlayer.events)
        assertEquals(expected = AudioPlayer.State.Paused, actual = audioPlayer.state.value)
    }

    /** Verifies that playing again while paused resumes playback. */
    @Test
    fun playResumesFromPaused() = runUnitTest {
        val audioPlayer = RecordingAudioPlayer()
        audioPlayer.setPlaylist(playlist = listOf("a"))
        audioPlayer.play()
        audioPlayer.pause()
        audioPlayer.events.clear()

        audioPlayer.play()

        assertEquals(expected = listOf("play"), actual = audioPlayer.events)
        assertEquals(expected = AudioPlayer.State.Playing, actual = audioPlayer.state.value)
    }

    /** Verifies that playing while already playing is ignored. */
    @Test
    fun playWhileAlreadyPlayingIsIgnored() = runUnitTest {
        val audioPlayer = RecordingAudioPlayer()
        audioPlayer.setPlaylist(playlist = listOf("a"))
        audioPlayer.play()
        audioPlayer.events.clear()

        audioPlayer.play()

        assertTrue(actual = audioPlayer.events.isEmpty())
        assertEquals(expected = AudioPlayer.State.Playing, actual = audioPlayer.state.value)
    }

    /** Verifies that stopping playback transitions back to idle. */
    @Test
    fun stopTransitionsToIdle() = runUnitTest {
        val audioPlayer = RecordingAudioPlayer()
        audioPlayer.setPlaylist(playlist = listOf("a"))
        audioPlayer.play()
        audioPlayer.events.clear()

        audioPlayer.stop()

        assertEquals(expected = listOf("stop"), actual = audioPlayer.events)
        assertEquals(expected = AudioPlayer.State.Idle, actual = audioPlayer.state.value)
    }

    /** Verifies that setting an equivalent playlist in a different order still restarts playback. */
    @Test
    fun setPlaylistRestartsEvenWhenReordered() = runUnitTest {
        val audioPlayer = RecordingAudioPlayer()
        audioPlayer.setPlaylist(playlist = listOf("a", "b"), loop = true)
        audioPlayer.events.clear()

        audioPlayer.setPlaylist(playlist = listOf("b", "a"), loop = true)

        assertEquals(expected = listOf("stop", "setPlaylist"), actual = audioPlayer.events)
    }

    /** Verifies that setting a different playlist restarts playback. */
    @Test
    fun setPlaylistRestartsWhenPlaylistDiffers() = runUnitTest {
        val audioPlayer = RecordingAudioPlayer()
        audioPlayer.setPlaylist(playlist = listOf("a", "b"), loop = true)
        audioPlayer.events.clear()

        audioPlayer.setPlaylist(playlist = listOf("c"), loop = true)

        assertEquals(expected = listOf("stop", "setPlaylist"), actual = audioPlayer.events)
    }

    /** Verifies that setting the same playlist with a different loop flag restarts playback. */
    @Test
    fun setPlaylistRestartsWhenLoopDiffers() = runUnitTest {
        val audioPlayer = RecordingAudioPlayer()
        audioPlayer.setPlaylist(playlist = listOf("a"), loop = true)
        audioPlayer.events.clear()

        audioPlayer.setPlaylist(playlist = listOf("a"), loop = false)

        assertEquals(expected = listOf("stop", "setPlaylist"), actual = audioPlayer.events)
    }

    /** Verifies that disabling the music feature flag stops playback immediately. */
    @Test
    fun disabledByFeatureFlagStopsImmediately() = runUnitTest {
        val audioPlayer = RecordingAudioPlayer()
        audioPlayer.setPlaylist(playlist = listOf("a"))
        audioPlayer.play()
        audioPlayer.events.clear()

        ClientFlags.set { it.copy(music = false) }
        audioPlayer.play()

        assertEquals(expected = listOf("stop"), actual = audioPlayer.events)
        assertEquals(expected = AudioPlayer.State.Idle, actual = audioPlayer.state.value)
    }

    /** Verifies that playback actions are ignored while the player reports itself as unavailable. */
    @Test
    fun unavailablePlayerIgnoresPlaybackActions() = runUnitTest {
        val audioPlayer = AudioPlayer()

        audioPlayer.setPlaylist(playlist = listOf("a"))
        audioPlayer.play()

        assertEquals(expected = AudioPlayer.State.Idle, actual = audioPlayer.state.value)
    }

    /** Verifies that [AudioPlayer.isPlaying] defaults to false. */
    @Test
    fun isPlayingDefaultsToFalse() = runUnitTest {
        val audioPlayer = RecordingAudioPlayer()

        assertFalse(actual = audioPlayer.callIsPlaying())
    }

    /** Verifies that disposing the player stops playback and releases platform resources. */
    @Test
    fun disposeStopsPlaybackAndReleasesPlatformResources() = runUnitTest {
        val audioPlayer = RecordingAudioPlayer()
        audioPlayer.setPlaylist(playlist = listOf("a"))
        audioPlayer.play()
        audioPlayer.events.clear()

        audioPlayer.dispose()

        assertEquals(expected = listOf("stop", "dispose"), actual = audioPlayer.events)
        assertEquals(expected = AudioPlayer.State.Idle, actual = audioPlayer.state.value)
    }

    /** Verifies that an exception thrown by the platform's play call is caught and force-stops playback. */
    @Test
    fun exceptionFromPlatformPlayForceStopsPlayback() = runUnitTest {
        val audioPlayer = object: AudioPlayer() {
            override val available: Boolean = true
            override fun platformPlay() {
                throw IllegalStateException("boom")
            }
        }

        audioPlayer.play()

        assertEquals(expected = AudioPlayer.State.Idle, actual = audioPlayer.state.value)
    }

    /** Verifies that an exception thrown by the platform's pause call is caught and force-stops playback. */
    @Test
    fun exceptionFromPlatformPauseForceStopsPlayback() = runUnitTest {
        val audioPlayer = object: AudioPlayer() {
            override val available: Boolean = true
            override fun platformPause() {
                throw IllegalStateException("boom")
            }
        }
        audioPlayer.setPlaylist(playlist = listOf("a"))
        audioPlayer.play()

        audioPlayer.pause()

        assertEquals(expected = AudioPlayer.State.Idle, actual = audioPlayer.state.value)
    }

    /** Verifies that [AudioPlayer.create] produces a usable player instance without throwing. */
    @Test
    fun createProducesAUsablePlayer() = runUnitTest {
        val audioPlayer = AudioPlayer.create()

        assertNotNull(actual = audioPlayer)
        assertEquals(expected = AudioPlayer.State.Idle, actual = audioPlayer.state.value)
    }
}
