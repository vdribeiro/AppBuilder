package com.app.builder.core.media

import javafx.scene.media.Media
import javafx.scene.media.MediaPlayer

/** Desktop [AudioPlayer] implementation backed by JavaFX [MediaPlayer]. */
internal class DesktopAudioPlayer: AudioPlayer() {

    /** The JavaFX [MediaPlayer] instance. */
    private var player: MediaPlayer? = null
    /** Index of the currently active track. */
    private var currentIndex = -1

    override val available: Boolean = true

    override fun isPlaying(): Boolean {
        super.isPlaying()
        return player?.status == MediaPlayer.Status.PLAYING
    }

    override fun platformSetPlaylist() {
        super.platformSetPlaylist()
        val nextIndex = currentIndex + 1
        if (!loop && nextIndex >= playlist.size) {
            stop()
            return
        }
        currentIndex = nextIndex % playlist.size

        val trackPath = playlist.getOrNull(index = currentIndex) ?: error(message = "Unable to get track at index $currentIndex")
        // Resources are resolved via the thread's context class loader, which on JVM points to the JAR's root where the resource files are placed.
        val resourceUrl = Thread.currentThread().contextClassLoader.getResource(trackPath) ?: error(message = "Unable to get resource for $trackPath")

        // The MediaPlayer is bound to a single Media source, so each track requires a new instance.
        // The previous player is disposed before creating the next one to release the underlying native media resources.
        player?.apply {
            stop()
            dispose()
        }
        player = MediaPlayer(Media(resourceUrl.toString())).apply {
            setOnEndOfMedia {
                platformSetPlaylist()
                platformPlay()
            }
        }
    }

    override fun platformPlay() {
        super.platformPlay()
        player?.play()
    }

    override fun platformPause() {
        super.platformPause()
        player?.pause()
    }

    override fun platformStop() {
        super.platformStop()
        player?.stop()
        player?.dispose()
        player = null
        currentIndex = -1
        playlist = emptyList()
    }

    /** The [MediaPlayer] is already released by [platformStop]; guards against a future player instance surviving past dispose. */
    override fun platformDispose() {
        super.platformDispose()
        player?.dispose()
        player = null
    }
}

internal actual fun createAudioPlayer(): AudioPlayer = DesktopAudioPlayer()
