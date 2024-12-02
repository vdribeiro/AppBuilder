package com.app.builder.data.resource

/**
 * A type-safe registry for audio assets, serving as a centralized index for background music and ambient tracks.
 * Eliminates the need for hardcoded string paths to reduce the risk of asset resolution errors.
 *
 * @property path The relative file path locating the audio asset within the `commonMain/resources/tracks` directory.
 */
sealed class AudioResource(val path: String) {
    /** A short notification ping sound. */
    data object Ping: AudioResource(path = "tracks/ping.mp3")
}
