package com.app.builder.core.media

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import com.app.builder.core.config.ClientFlags
import com.app.builder.test.TestCase

class CameraTest: TestCase() {

    /**
     * A [Camera] that reports itself as available and permitted, so the public lifecycle methods are not short-circuited by their guards.
     *
     * @return A [Camera] that always reports itself available and permitted.
     */
    private fun testCamera(): Camera = object: Camera() {
        override val available: Boolean = true
        override fun hasPermission(): Boolean = true
    }

    /**
     * A [testCamera] whose recording hooks succeed synchronously, for tests that need a genuine [Camera.State.Recording] transition.
     *
     * @return A [Camera] whose recording hooks succeed synchronously.
     */
    private fun recordingCamera(): Camera = object: Camera() {
        /** The callback given to [platformStartRecording], invoked by [platformStopRecording]. */
        private var onResult: ((String) -> Unit)? = null

        override val available: Boolean = true
        override fun hasPermission(): Boolean = true

        override fun platformStartRecording(onResult: (String) -> Unit) {
            this.onResult = onResult
        }

        override fun platformStopRecording() {
            super.platformStopRecording()
            onResult?.invoke("/video.mp4")
        }
    }

    /** Verifies that a new [Camera] starts in the idle state. */
    @Test
    fun initialStateIsIdle() = runUnitTest {
        val camera = Camera()
        assertEquals(expected = Camera.State.Idle, actual = camera.state.value)
    }

    /** Verifies that starting the preview transitions the camera to previewing. */
    @Test
    fun startPreviewTransitionsToPreviewing() = runUnitTest {
        val camera = testCamera()
        camera.startPreview()
        assertEquals(expected = Camera.State.Previewing, actual = camera.state.value)
    }

    /** Verifies that stopping the preview transitions the camera back to idle. */
    @Test
    fun stopPreviewTransitionsToIdle() = runUnitTest {
        val camera = testCamera()
        camera.startPreview()
        camera.stopPreview()
        assertEquals(expected = Camera.State.Idle, actual = camera.state.value)
    }

    /** Verifies that starting recording while previewing transitions the camera to recording. */
    @Test
    fun startRecordingTransitionsToRecording() = runUnitTest {
        val camera = recordingCamera()
        camera.startPreview()
        camera.startRecording(onResult = {})
        assertEquals(expected = Camera.State.Recording, actual = camera.state.value)
    }

    /** Verifies that stopping recording transitions the camera back to previewing. */
    @Test
    fun stopRecordingTransitionsBackToPreviewing() = runUnitTest {
        val camera = recordingCamera()
        camera.startPreview()
        camera.startRecording(onResult = {})
        camera.stopRecording()
        assertEquals(expected = Camera.State.Previewing, actual = camera.state.value)
    }

    /** Verifies that stopping the preview while recording transitions the camera directly to idle. */
    @Test
    fun stopPreviewWhileRecordingTransitionsToIdle() = runUnitTest {
        val camera = recordingCamera()
        camera.startPreview()
        camera.startRecording(onResult = {})
        camera.stopPreview()
        assertEquals(expected = Camera.State.Idle, actual = camera.state.value)
    }

    /** Verifies that toggling the camera facing is ignored unless previewing, and otherwise does not change the current state. */
    @Test
    fun toggleFacing() = runUnitTest {
        val camera = testCamera()
        camera.toggleFacing()
        assertEquals(expected = Camera.State.Idle, actual = camera.state.value)
        camera.startPreview()
        camera.toggleFacing()
        assertEquals(expected = Camera.State.Previewing, actual = camera.state.value)
    }

    /** Verifies that capturing a photo while previewing keeps the camera in previewing. */
    @Test
    fun capturePhotoWhilePreviewingKeepsPreviewing() = runUnitTest {
        val camera = testCamera()
        camera.startPreview()
        camera.capturePhoto(onResult = {})
        assertEquals(expected = Camera.State.Previewing, actual = camera.state.value)
    }

    /** Verifies that calls invalid for the current state leave the state untouched, with capturePhoto reporting the failure as a null result rather than being silently dropped. */
    @Test
    fun invalidCallsAreIgnored() = runUnitTest {
        val camera = recordingCamera()

        var photo: String? = "not yet invoked"
        camera.capturePhoto { photo = it }
        assertNull(actual = photo)
        assertEquals(expected = Camera.State.Idle, actual = camera.state.value)

        camera.startRecording(onResult = {})
        assertEquals(expected = Camera.State.Idle, actual = camera.state.value)
        camera.stopRecording()
        assertEquals(expected = Camera.State.Idle, actual = camera.state.value)
        camera.stopPreview()
        assertEquals(expected = Camera.State.Idle, actual = camera.state.value)

        camera.startPreview()
        assertEquals(expected = Camera.State.Previewing, actual = camera.state.value)
        camera.startPreview()
        assertEquals(expected = Camera.State.Previewing, actual = camera.state.value)

        camera.startRecording(onResult = {})
        assertEquals(expected = Camera.State.Recording, actual = camera.state.value)
        camera.startRecording(onResult = {})
        assertEquals(expected = Camera.State.Recording, actual = camera.state.value)
    }

    /** Verifies that the platform camera handles every call without throwing. */
    @Test
    fun platformCameraHandlesEveryCallWithoutThrowing() = runUnitTest {
        val camera = createCamera()

        camera.startPreview()
        camera.toggleFacing()
        camera.capturePhoto(onResult = {})
        camera.startRecording(onResult = {})
        camera.stopRecording()
        camera.toggleFacing()
        camera.stopPreview()

        assertEquals(expected = Camera.State.Idle, actual = camera.state.value)
    }

    /** Verifies that [Camera.capturePhoto] and [Camera.startRecording] each deliver the path from the platform implementation to their `onResult` callback. */
    @Test
    fun captureAndRecordingDeliverTheirPathToTheCaller() = runUnitTest {
        val camera = object: Camera() {
            /** The callback given to [platformStartRecording], invoked by [platformStopRecording]. */
            private var onResult: ((String) -> Unit)? = null

            override val available: Boolean = true
            override fun hasPermission(): Boolean = true

            override fun platformCapturePhoto(onResult: (String?) -> Unit) {
                onResult("/photo.jpg")
            }

            override fun platformStartRecording(onResult: (String) -> Unit) {
                this.onResult = onResult
            }

            override fun platformStopRecording() {
                super.platformStopRecording()
                onResult?.invoke("/video.mp4")
            }
        }

        camera.startPreview()
        var photo: String? = null
        camera.capturePhoto { photo = it }

        var video: String? = null
        camera.startRecording { video = it }
        camera.stopRecording()

        assertEquals(expected = "/photo.jpg", actual = photo)
        assertEquals(expected = "/video.mp4", actual = video)
    }

    /** Verifies that disabling the camera feature flag forces the camera back to idle. */
    @Test
    fun disabledFlagForcesIdle() = runUnitTest {
        val camera = testCamera()
        camera.startPreview()
        assertEquals(expected = Camera.State.Previewing, actual = camera.state.value)

        ClientFlags.set { it.copy(camera = false) }
        var photo: String? = "not yet invoked"
        camera.capturePhoto { photo = it }
        assertNull(actual = photo)
        assertEquals(expected = Camera.State.Idle, actual = camera.state.value)

        camera.startPreview()
        assertEquals(expected = Camera.State.Idle, actual = camera.state.value)
    }

    /** Verifies that starting the preview is ignored while the camera lacks permission, even if it is available. */
    @Test
    fun startPreviewIgnoredWithoutPermission() = runUnitTest {
        val camera = object: Camera() {
            override val available: Boolean = true
            override fun hasPermission(): Boolean = false
        }

        camera.startPreview()

        assertEquals(expected = Camera.State.Idle, actual = camera.state.value)
    }

    /** Verifies that disposing the camera stops any active session and releases platform resources. */
    @Test
    fun disposeStopsPreviewAndReleasesPlatformResources() = runUnitTest {
        var platformDisposed = false
        val camera = object: Camera() {
            override val available: Boolean = true
            override fun hasPermission(): Boolean = true
            override fun platformDispose() {
                platformDisposed = true
            }
        }
        camera.startPreview()

        camera.dispose()

        assertEquals(expected = Camera.State.Idle, actual = camera.state.value)
        assertTrue(actual = platformDisposed)
    }

    /** Verifies that toggling the camera facing while previewing actually flips the active lens direction. */
    @Test
    fun toggleFacingFlipsTheActiveLensDirection() = runUnitTest {
        var reportedFacing: Camera.Facing? = null
        val camera = object: Camera() {
            override val available: Boolean = true
            override fun hasPermission(): Boolean = true
            override fun platformToggleFacing() {
                reportedFacing = facing.value
            }
        }
        camera.startPreview()

        camera.toggleFacing()
        assertEquals(expected = Camera.Facing.FRONT, actual = reportedFacing)

        camera.toggleFacing()
        assertEquals(expected = Camera.Facing.BACK, actual = reportedFacing)
    }

    /** Verifies that an exception thrown by a platform call is caught and recovers the camera back to idle. */
    @Test
    fun exceptionFromPlatformCallRecoversToIdle() = runUnitTest {
        val camera = object: Camera() {
            override val available: Boolean = true
            override fun hasPermission(): Boolean = true
            override fun platformStartPreview() {
                throw IllegalStateException("boom")
            }
        }

        camera.startPreview()

        assertEquals(expected = Camera.State.Idle, actual = camera.state.value)
    }

    /** Verifies that [Camera.hasPermission] defaults to false. */
    @Test
    fun hasPermissionDefaultsToFalse() = runUnitTest {
        assertFalse(actual = Camera().hasPermission())
    }

    /** Verifies that disabling the camera feature flag during a session makes recording and facing actions stop the session instead of running. */
    @Test
    fun disabledFlagStopsRecordingAndFacingActions() = runUnitTest {
        val camera = testCamera()
        val actions: List<() -> Unit> = listOf(
            { camera.startRecording(onResult = {}) },
            { camera.stopRecording() },
            { camera.toggleFacing() },
        )

        actions.forEach { action ->
            ClientFlags.set { it.copy(camera = true) }
            camera.startPreview()
            assertEquals(expected = Camera.State.Previewing, actual = camera.state.value)

            ClientFlags.set { it.copy(camera = false) }
            action()

            assertEquals(expected = Camera.State.Idle, actual = camera.state.value)
        }
        assertEquals(expected = Camera.Facing.BACK, actual = camera.facing.value)
    }

    /** Verifies that an unavailable camera ignores every action, answering a photo capture with null and keeping the default lens. */
    @Test
    fun unavailableCameraIgnoresEveryAction() = runUnitTest {
        val camera = Camera()

        var photo: String? = "not yet invoked"
        camera.capturePhoto { photo = it }
        camera.startRecording(onResult = {})
        camera.stopRecording()
        camera.toggleFacing()

        assertNull(actual = photo)
        assertEquals(expected = Camera.State.Idle, actual = camera.state.value)
        assertEquals(expected = Camera.Facing.BACK, actual = camera.facing.value)
    }

    /** Verifies that revoking the camera permission during a session makes every action stop the session instead of running. */
    @Test
    fun revokedPermissionStopsTheSession() = runUnitTest {
        var permitted = true
        val camera = object: Camera() {
            override val available: Boolean = true
            override fun hasPermission(): Boolean = permitted
        }
        val actions: List<() -> Unit> = listOf(
            { camera.capturePhoto {} },
            { camera.startRecording(onResult = {}) },
            { camera.stopRecording() },
            { camera.toggleFacing() },
        )

        actions.forEach { action ->
            permitted = true
            camera.startPreview()
            assertEquals(expected = Camera.State.Previewing, actual = camera.state.value)

            permitted = false
            action()

            assertEquals(expected = Camera.State.Idle, actual = camera.state.value)
        }
        assertEquals(expected = Camera.Facing.BACK, actual = camera.facing.value)
    }

    /** Verifies that an exception thrown by the platform's dispose call is caught after the session has been stopped. */
    @Test
    fun exceptionFromPlatformDisposeIsCaught() = runUnitTest {
        val camera = object: Camera() {
            override val available: Boolean = true
            override fun hasPermission(): Boolean = true
            override fun platformDispose() {
                throw IllegalStateException("boom")
            }
        }
        camera.startPreview()

        camera.dispose()

        assertEquals(expected = Camera.State.Idle, actual = camera.state.value)
    }

    /** Verifies that an exception thrown by the platform's stop preview call is caught and the camera still transitions to idle. */
    @Test
    fun exceptionFromPlatformStopPreviewIsCaught() = runUnitTest {
        var failStop = false
        val camera = object: Camera() {
            override val available: Boolean = true
            override fun hasPermission(): Boolean = true
            override fun platformStopPreview() {
                if (failStop) throw IllegalStateException("boom")
            }
        }
        camera.startPreview()
        failStop = true

        camera.stopPreview()

        assertEquals(expected = Camera.State.Idle, actual = camera.state.value)
    }

    /** Verifies that an exception thrown by the platform's photo capture is caught and answered with null. */
    @Test
    fun exceptionFromPlatformCapturePhotoAnswersNull() = runUnitTest {
        val camera = object: Camera() {
            override val available: Boolean = true
            override fun hasPermission(): Boolean = true
            override fun platformCapturePhoto(onResult: (String?) -> Unit) {
                throw IllegalStateException("boom")
            }
        }
        camera.startPreview()

        var photo: String? = "not yet invoked"
        camera.capturePhoto { photo = it }

        assertNull(actual = photo)
    }

    /** Verifies that an exception thrown by the platform's start recording call is caught and the camera keeps previewing. */
    @Test
    fun exceptionFromPlatformStartRecordingKeepsPreviewing() = runUnitTest {
        val camera = object: Camera() {
            override val available: Boolean = true
            override fun hasPermission(): Boolean = true
            override fun platformStartRecording(onResult: (String) -> Unit) {
                throw IllegalStateException("boom")
            }
        }
        camera.startPreview()

        camera.startRecording(onResult = {})

        assertEquals(expected = Camera.State.Previewing, actual = camera.state.value)
    }

    /** Verifies that an exception thrown by the platform's stop recording call is caught and the camera stays recording, since the recording did not stop. */
    @Test
    fun exceptionFromPlatformStopRecordingKeepsRecording() = runUnitTest {
        var failStop = false
        val camera = object: Camera() {
            override val available: Boolean = true
            override fun hasPermission(): Boolean = true
            override fun platformStopRecording() {
                if (failStop) throw IllegalStateException("boom")
            }
        }
        camera.startPreview()
        camera.startRecording(onResult = {})
        failStop = true

        camera.stopRecording()

        assertEquals(expected = Camera.State.Recording, actual = camera.state.value)
    }

    /** Verifies that an exception thrown by the platform's facing toggle is caught, restores the previous lens, and keeps the camera previewing. */
    @Test
    fun exceptionFromPlatformToggleFacingRestoresTheLens() = runUnitTest {
        val camera = object: Camera() {
            override val available: Boolean = true
            override fun hasPermission(): Boolean = true
            override fun platformToggleFacing() {
                throw IllegalStateException("boom")
            }
        }
        camera.startPreview()

        camera.toggleFacing()

        assertEquals(expected = Camera.State.Previewing, actual = camera.state.value)
        assertEquals(expected = Camera.Facing.BACK, actual = camera.facing.value)
    }

    /** Verifies that [Camera.create] produces a usable camera instance without throwing. */
    @Test
    fun createProducesAUsableCamera() = runUnitTest {
        val camera = Camera.create()

        assertNotNull(actual = camera)
        assertEquals(expected = Camera.State.Idle, actual = camera.state.value)
    }
}
