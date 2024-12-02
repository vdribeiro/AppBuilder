package com.app.builder.core.media

import android.content.pm.PackageManager
import androidx.core.net.toUri
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.app.builder.applicationContext
import com.app.builder.core.telemetry.Telemetry

/** Android [AudioPlayer] implementation backed by Media3's [ExoPlayer]. */
internal class AndroidAudioPlayer: AudioPlayer() {

    /** The ExoPlayer instance. */
    private val player: ExoPlayer = ExoPlayer.Builder(applicationContext).build()

    override val available: Boolean = runCatching {
        applicationContext.packageManager.hasSystemFeature(PackageManager.FEATURE_AUDIO_OUTPUT)
    }.onFailure {
        Telemetry.error(tag = TAG, message = "Unable to check audio player availability", throwable = it)
    }.getOrDefault(defaultValue = false)

    override fun isPlaying(): Boolean {
        super.isPlaying()
        return player.isPlaying
    }

    override fun platformSetPlaylist() {
        super.platformSetPlaylist()
        // The `asset:///` URI scheme maps playlist entries to files bundled in the `assets/` folder.
        val mediaItems = playlist.map { MediaItem.fromUri("asset:///$it".toUri()) }
        player.apply {
            stop()
            setMediaItems(mediaItems)
            repeatMode = if (loop) Player.REPEAT_MODE_ALL else Player.REPEAT_MODE_OFF
            prepare()
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
        player.stop()
        playlist = emptyList()
    }

    override fun platformDispose() {
        super.platformDispose()
        player.release()
    }

    companion object {
        private const val TAG = "AndroidAudioPlayer"
    }
}

internal actual fun createAudioPlayer(): AudioPlayer = AndroidAudioPlayer()
