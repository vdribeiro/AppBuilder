package com.app.builder.data.resource

import kotlin.test.Test
import kotlin.test.assertEquals
import com.app.builder.test.TestCase

class ResourceTest: TestCase() {

    /** Verifies that [AudioResource.Ping] resolves to its expected file path. */
    @Test
    fun audioResource() = runUnitTest {
        assertEquals(expected = "tracks/ping.mp3", actual = AudioResource.Ping.path)
    }

    /** Verifies that [ImageResource.Launcher] resolves to its expected file path. */
    @Test
    fun imageResource() = runUnitTest {
        assertEquals(expected = "drawable/ic_launcher.png", actual = ImageResource.Launcher.path)
    }

    /** Verifies that [JsonResource.Translations] resolves to its expected file path. */
    @Test
    fun jsonResource() = runUnitTest {
        assertEquals(expected = "files/translations.json", actual = JsonResource.Translations.path)
    }
}
