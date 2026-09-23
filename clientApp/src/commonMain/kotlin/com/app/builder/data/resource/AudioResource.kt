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
    /** An arpeggio climbing and falling back through a major chord. */
    data object Chime: AudioResource(path = "tracks/chime.mp3")
    /** A low note re-struck at a steady beat. */
    data object Pulse: AudioResource(path = "tracks/pulse.mp3")
    /** A sustained ambient chord under a slow tremolo. */
    data object Drone: AudioResource(path = "tracks/drone.mp3")
}
