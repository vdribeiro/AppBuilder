package com.app.builder.core.media

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import com.app.builder.core.config.ClientFlags
import com.app.builder.core.telemetry.Telemetry

/** Audio player intended to be extended by platform-specific implementations. */
open class AudioPlayer {

    /** Observable state of audio playback. */
    sealed interface State {
        /** No active playlist. */
        data object Idle: State
        /** Playback is active. */
        data object Playing: State
        /** Playback is paused. */
        data object Paused: State
    }

    /** The currently active playlist. */
    protected var playlist = listOf<String>()

    /** If the playlist repeats indefinitely. */
    protected var loop: Boolean = false

    /** If the audio player is available on this device. */
    open val available: Boolean = false

    /** Backing state for [state]. */
    private val _state: MutableStateFlow<State> = MutableStateFlow(value = State.Idle)
    /** Observable state of audio playback. */
    val state: StateFlow<State> = _state.asStateFlow()

    /**
     * Set the [playlist].
     *
     * @param playlist The resources to play.
     * @param loop If true, repeats the playlist indefinitely.
     * @param shuffle If true, randomizes the playback order.
     */
    fun setPlaylist(playlist: List<String>, loop: Boolean = false, shuffle: Boolean = false) {
        runCatching {
            if (!ClientFlags.flags.music || !available) return@runCatching stop()
            stop()
            this.playlist = if (shuffle) playlist.shuffled() else playlist
            this.loop = loop
            platformSetPlaylist()
        }.onFailure {
            Telemetry.error(tag = TAG, message = "Unable to set playlist", throwable = it)
        }
    }

    /** Starts or resumes playback. Transitions to [State.Playing]. */
    fun play() {
        runCatching {
            if (!ClientFlags.flags.music || !available) return@runCatching stop()
            when (state.value) {
                State.Playing -> return
                State.Idle,
                State.Paused -> platformPlay()
            }
            _state.update { State.Playing }
        }.onFailure {
            Telemetry.error(tag = TAG, message = "Error starting playback", throwable = it)
            stop()
        }
    }

    /** Pauses playback. */
    fun pause() {
        runCatching {
            if (!ClientFlags.flags.music || !available) return@runCatching stop()
            platformPause()
            _state.update { State.Paused }
        }.onFailure {
            Telemetry.error(tag = TAG, message = "Error pausing playback", throwable = it)
            stop()
        }
    }

    /** Stops playback. */
    fun stop() {
        runCatching {
            platformStop()
        }.onFailure {
            Telemetry.error(tag = TAG, message = "Error stopping playback", throwable = it)
        }
        _state.update { State.Idle }
    }

    /** Stops playback and releases platform resources. */
    fun dispose() {
        stop()
        runCatching {
            platformDispose()
        }.onFailure {
            Telemetry.error(tag = TAG, message = "Error disposing audio player", throwable = it)
        }
    }

    /**
     * Checks if audio is actively playing.
     *
     * @return `true` if playing, `false` otherwise.
     */
    protected open fun isPlaying(): Boolean = false

    /** Called when playlist is set. */
    protected open fun platformSetPlaylist() {}

    /** Starts playback of the current [playlist]. */
    protected open fun platformPlay() {}

    /** Pauses playback. */
    protected open fun platformPause() {}

    /** Stops playback. */
    protected open fun platformStop() {}

    /** Release platform resources. */
    protected open fun platformDispose() {}

    companion object {
        private const val TAG = "AudioPlayer"

        /**
         * Creates an [AudioPlayer].
         *
         * @return [AudioPlayer] instance.
         */
        internal fun create(): AudioPlayer = runCatching {
            createAudioPlayer().also { Telemetry.info(tag = TAG, message = "Audio Player created") }
        }.onFailure {
            Telemetry.error(tag = TAG, message = "Unable to create the Audio Player and returning a no-op player", throwable = it)
        }.getOrDefault(defaultValue = AudioPlayer())
    }
}

/**
 * Creates a platform-specific [AudioPlayer].
 *
 * @return A new instance of [AudioPlayer].
 */
internal expect fun createAudioPlayer(): AudioPlayer
