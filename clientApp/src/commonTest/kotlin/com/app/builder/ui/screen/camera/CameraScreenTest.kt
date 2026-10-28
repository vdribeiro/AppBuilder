package com.app.builder.ui.screen.camera

import kotlin.test.Test
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performSemanticsAction
import com.app.builder.Dependency.getUserDependency
import com.app.builder.core.media.Camera
import com.app.builder.core.platform.OS
import com.app.builder.core.platform.platform
import com.app.builder.test.FakeData
import com.app.builder.test.TestCase
import com.app.builder.ui.LocalCamera
import com.app.builder.ui.component.actionbar.ActionBarState
import com.app.builder.ui.component.actionbar.ActionBarStore
import com.app.builder.ui.component.navigation.NavigationState
import com.app.builder.ui.component.navigation.NavigationStore
import com.app.builder.ui.permission.LocalPermissionManager
import com.app.builder.ui.permission.Permission
import com.app.builder.ui.permission.PermissionManager

/** Reports itself as available and permitted, answering captures with fixed paths, without reaching any platform camera API. */
private class FakeCamera: Camera() {
    override val available: Boolean = true

    /** The callback of the recording in progress, if any. */
    private var onRecordingResult: ((String) -> Unit)? = null

    override fun hasPermission(): Boolean = true

    override fun platformCapturePhoto(onResult: (String?) -> Unit) = onResult("photo.jpg")

    override fun platformStartRecording(onResult: (String) -> Unit) {
        onRecordingResult = onResult
    }

    override fun platformStopRecording() {
        onRecordingResult?.invoke("video.mp4")
        onRecordingResult = null
    }
}

/** Grants every permission without reaching any platform permission API. */
private class GrantingPermissionManager: PermissionManager() {
    override suspend fun hasPermission(permission: Permission): Boolean = true
}

class CameraScreenTest: TestCase() {

    /** Verifies that the camera screen renders its bars and that, on platforms with camera controls, the controls follow the camera's state. */
    @Test
    fun cameraScreen() = runUITest {
        val authenticatedUseCases = dependency.get()
            .getUserDependency(user = FakeData.adminUser)
            .useCases
        authenticatedUseCases.authenticationUseCases.login(credentials = FakeData.adminCredentials)

        val actionBarStore = ActionBarStore(state = ActionBarState(), router = router, authenticationUseCases = authenticatedUseCases.authenticationUseCases)
        val navigationStore = NavigationStore(state = NavigationState(), router = router, authenticationUseCases = authenticatedUseCases.authenticationUseCases)
        val camera = FakeCamera()

        setUI {
            CompositionLocalProvider(
                LocalCamera provides camera,
                LocalPermissionManager provides GrantingPermissionManager(),
            ) {
                CameraScreen(actionBarStore = actionBarStore, navigationStore = navigationStore)
            }
        }

        onNodeWithTag(testTag = "action_bar").assertIsDisplayed()
        onNodeWithTag(testTag = "navigation_bar").assertIsDisplayed()

        // Desktop platforms only show the preview, without controls.
        if (platform.os == OS.Windows || platform.os == OS.Mac || platform.os == OS.Linux) {
            onNodeWithText(text = "Start").assertDoesNotExist()
            return@runUITest
        }

        // Idle: only Start is offered.
        onNodeWithText(text = "Start").assertIsEnabled()
        onNodeWithText(text = "Stop").assertIsNotEnabled()
        onNodeWithText(text = "Front").assertIsNotEnabled()
        onNodeWithText(text = "Photo").assertIsNotEnabled()
        onNodeWithText(text = "Record").assertIsNotEnabled()
        onNodeWithText(text = "Stop recording").assertIsNotEnabled()

        // Previewing: the preview can be stopped, flipped, photographed or recorded.
        onNodeWithText(text = "Start").performClick()
        waitUntil { camera.state.value == Camera.State.Previewing }

        onNodeWithText(text = "Start").assertIsNotEnabled()
        onNodeWithText(text = "Stop").assertIsEnabled()
        onNodeWithText(text = "Photo").assertIsEnabled()
        onNodeWithText(text = "Record").assertIsEnabled()
        onNodeWithText(text = "Stop recording").assertIsNotEnabled()

        // Toggling the lens relabels the button with the other lens.
        onNodeWithText(text = "Front").performClick()
        waitUntil { camera.facing.value == Camera.Facing.FRONT }
        onNodeWithText(text = "Back").assertIsEnabled()

        // A captured photo is reported in the snackbar.
        onNodeWithText(text = "Photo").performClick()
        onNodeWithText(text = "photo.jpg").assertIsDisplayed()

        // The snackbar overlaps the controls from here on, so they are clicked through their semantics instead of a pointer tap.
        // Recording: only Stop recording is offered among the capture controls.
        onNodeWithText(text = "Record").performSemanticsAction(key = SemanticsActions.OnClick)
        waitUntil { camera.state.value == Camera.State.Recording }

        onNodeWithText(text = "Photo").assertIsNotEnabled()
        onNodeWithText(text = "Record").assertIsNotEnabled()
        onNodeWithText(text = "Stop recording").assertIsEnabled()

        // Stopping the recording reports the video and returns to the preview.
        onNodeWithText(text = "Stop recording").performSemanticsAction(key = SemanticsActions.OnClick)
        waitUntil { camera.state.value == Camera.State.Previewing }
        onNodeWithText(text = "video.mp4").assertIsDisplayed()

        // Stopped: back to the idle controls.
        onNodeWithText(text = "Stop").performSemanticsAction(key = SemanticsActions.OnClick)
        waitUntil { camera.state.value == Camera.State.Idle }

        onNodeWithText(text = "Start").assertIsEnabled()
        onNodeWithText(text = "Stop").assertIsNotEnabled()
    }
}
