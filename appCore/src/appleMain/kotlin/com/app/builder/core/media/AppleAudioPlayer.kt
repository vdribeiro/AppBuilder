package com.app.builder.core.media

import platform.AVFoundation.AVPlayer
import platform.AVFoundation.AVPlayerItem
import platform.AVFoundation.AVPlayerItemDidPlayToEndTimeNotification
import platform.AVFoundation.pause
import platform.AVFoundation.play
import platform.AVFoundation.rate
import platform.AVFoundation.replaceCurrentItemWithPlayerItem
import platform.Foundation.NSBundle
import platform.Foundation.NSNotificationCenter
import platform.darwin.NSObjectProtocol
import com.app.builder.core.platform.observe

/** Apple [AudioPlayer] implementation backed by AVFoundation's [AVPlayer]. */
internal class AppleAudioPlayer: AudioPlayer() {

    /** The AVFoundation's [AVPlayer] instance. */
    private val player: AVPlayer = AVPlayer()
    /** Index of the currently active track. */
    private var currentIndex = -1
    /** Observer registered for the current track's end-of-playback notification, used to advance to the next track. */
    private var endOfSongObserver: NSObjectProtocol? = null

    override val available: Boolean = true

    override fun isPlaying(): Boolean {
        super.isPlaying()
        return player.rate != 0.0f
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
        // Track resources are resolved from the main app bundle, which is where the resource files are copied to.
        val resourceUrl = NSBundle.mainBundle.URLForResource(
            name = trackPath.substringBeforeLast(delimiter = '.'),
            withExtension = trackPath.substringAfterLast(delimiter = '.'),
        ) ?: error(message = "Unable to get resource $trackPath")
        val playerItem = AVPlayerItem(uRL = resourceUrl)

        // Remove the previous observer before registering a new one to avoid duplicate callbacks.
        endOfSongObserver?.let { NSNotificationCenter.defaultCenter.removeObserver(observer = it) }
        endOfSongObserver = NSNotificationCenter.defaultCenter.observe(
            name = AVPlayerItemDidPlayToEndTimeNotification,
            key = playerItem,
        ) {
            platformSetPlaylist()
            platformPlay()
        }
        player.replaceCurrentItemWithPlayerItem(item = playerItem)
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
        player.pause()
        player.replaceCurrentItemWithPlayerItem(item = null)
        endOfSongObserver?.let { NSNotificationCenter.defaultCenter.removeObserver(observer = it) }
        endOfSongObserver = null
        currentIndex = -1
        playlist = emptyList()
    }

    /** AVPlayer requires no explicit release call; ensures the notification observer is not left registered past the player's lifetime. */
    override fun platformDispose() {
        super.platformDispose()
        player.replaceCurrentItemWithPlayerItem(item = null)
        endOfSongObserver?.let { NSNotificationCenter.defaultCenter.removeObserver(observer = it) }
        endOfSongObserver = null
    }
}

internal actual fun createAudioPlayer(): AudioPlayer = AppleAudioPlayer()
