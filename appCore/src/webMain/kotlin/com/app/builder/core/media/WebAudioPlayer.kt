package com.app.builder.core.media

import kotlinx.browser.document
import org.w3c.dom.HTMLAudioElement

/** Web [AudioPlayer] implementation backed by an [HTMLAudioElement]. */
internal class WebAudioPlayer: AudioPlayer() {

    /** The [HTMLAudioElement] instance reused across tracks. `preload = "auto"` hints to the browser to buffer the next chunk proactively. */
    private val player: HTMLAudioElement = (document.createElement(localName = "audio") as HTMLAudioElement).apply { preload = "auto" }
    /** Index of the currently active track. */
    private var currentIndex = -1

    override val available: Boolean = true

    override fun isPlaying(): Boolean {
        super.isPlaying()
        return !player.paused && !player.ended
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

        player.apply {
            src = trackPath
            onended = {
                platformSetPlaylist()
                platformPlay()
            }
        }
    }

    override fun platformPlay() {
        super.platformPlay()
        player.play()
    }

    override fun platformPause() {
        super.platformPause()
        player.pause()
    }

    override fun platformStop() {
        super.platformStop()
        // Pausing and setting currentTime may not stop buffering.
        // We need to remove 'src' and call load() to abort any pending network request and reset the element to an idle state.
        player.pause()
        player.currentTime = 0.0
        player.removeAttribute(qualifiedName = "src")
        player.load()
        currentIndex = -1
        playlist = emptyList()
    }

    /** Detaches the end-of-track handler so the element no longer holds a closure referencing this player. */
    override fun platformDispose() {
        super.platformDispose()
        player.onended = null
    }
}

internal actual fun createAudioPlayer(): AudioPlayer = WebAudioPlayer()
