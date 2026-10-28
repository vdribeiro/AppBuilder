package com.app.builder.ui.media

import kotlin.test.Test
import kotlin.test.assertNotNull
import com.app.builder.core.media.Camera
import com.app.builder.test.TestCase

class CameraTest: TestCase() {

    /** Verifies that [rememberCamera] provides a non-null [Camera]. */
    @Test
    fun rememberCameraProvidesADefaultCamera() = runUITest {
        var camera: Camera? = null

        setUI { camera = rememberCamera() }

        assertNotNull(actual = camera)
    }

    /** Verifies that [CameraPreview] renders without failing when given a camera that is not the platform's own. */
    @Test
    fun cameraPreviewRendersForANonPlatformCamera() = runUITest {
        setUI { CameraPreview(camera = Camera()) }
    }
}
