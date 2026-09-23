package com.app.builder.ui.screen.camera

import kotlinx.coroutines.launch
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.app.builder.core.media.Camera
import com.app.builder.core.platform.OS
import com.app.builder.core.platform.platform
import com.app.builder.ui.LocalCamera
import com.app.builder.ui.Preview
import com.app.builder.ui.component.actionbar.ActionBar
import com.app.builder.ui.component.actionbar.ActionBarAction
import com.app.builder.ui.component.actionbar.ActionBarState
import com.app.builder.ui.component.navigation.Navigation
import com.app.builder.ui.component.navigation.NavigationState
import com.app.builder.ui.core.button.Button
import com.app.builder.ui.core.button.ButtonStyle
import com.app.builder.ui.core.overlay.Snackbar
import com.app.builder.ui.core.text.Text
import com.app.builder.ui.media.CameraPreview
import com.app.builder.ui.permission.LocalPermissionManager
import com.app.builder.ui.permission.Permission
import com.app.builder.ui.screen.Screen
import com.app.builder.ui.store.Store

/**
 * The Camera Screen.
 *
 * @param actionBarStore The store for action bar.
 * @param navigationStore Drives the bottom navigation bar.
 */
@Composable
fun CameraScreen(
    actionBarStore: Store<ActionBarState, ActionBarAction>,
    navigationStore: Store<NavigationState, Unit>,
) {
    val permissionManager = LocalPermissionManager.current
    val camera = LocalCamera.current
    val coroutineScope = rememberCoroutineScope()

    val state by camera.state.collectAsStateWithLifecycle()
    val facing by camera.facing.collectAsStateWithLifecycle()

    var snackbarMessage by remember { mutableStateOf<String?>(value = null) }

    Screen(
        topBar = { ActionBar(store = actionBarStore) },
        bottomBar = { Navigation(store = navigationStore) },
        snackbarHost = {
            snackbarMessage?.let {
                Snackbar(message = it, onDismiss = { snackbarMessage = null })
            }
        }
    ) {
        CameraPreview(modifier = Modifier.fillMaxSize(), camera = camera)

        if (platform.os == OS.Windows || platform.os == OS.Mac || platform.os == OS.Linux) return@Screen
        Column(
            modifier = Modifier
                .align(alignment = Alignment.BottomCenter)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(space = 8.dp)
        ) {
            Text(text = state.toString())

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(space = 8.dp)
            ) {
                Button(
                    modifier = Modifier.weight(weight = 1f),
                    style = ButtonStyle.FILLED,
                    text = "Start",
                    enabled = state == Camera.State.Idle,
                ) {
                    coroutineScope.launch {
                        if (permissionManager.grantPermission(permission = Permission.CAMERA)) {
                            camera.startPreview()
                        }
                    }
                }
                Button(
                    modifier = Modifier.weight(weight = 1f),
                    style = ButtonStyle.FILLED,
                    text = "Stop",
                    enabled = state != Camera.State.Idle,
                ) {
                    camera.stopPreview()
                }
                Button(
                    modifier = Modifier.weight(weight = 1f),
                    style = ButtonStyle.FILLED,
                    text = "Photo",
                    enabled = state == Camera.State.Previewing,
                ) {
                    camera.capturePhoto { path -> snackbarMessage = path }
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(space = 8.dp)
            ) {
                Button(
                    modifier = Modifier.weight(weight = 1f),
                    style = ButtonStyle.FILLED,
                    text = "Record",
                    enabled = state == Camera.State.Previewing,
                ) {
                    camera.startRecording { path -> snackbarMessage = path }
                }
                Button(
                    modifier = Modifier.weight(weight = 1f),
                    style = ButtonStyle.FILLED,
                    text = "Stop recording",
                    enabled = state == Camera.State.Recording,
                ) {
                    camera.stopRecording()
                }
                Button(
                    modifier = Modifier.weight(weight = 1f),
                    style = ButtonStyle.FILLED,
                    text = if (facing == Camera.Facing.BACK) "Front" else "Back",
                    enabled = state == Camera.State.Previewing,
                ) {
                    camera.toggleFacing()
                }
            }
        }
    }
}

@Preview
@Composable
private fun CameraScreenPreview() = Preview {
    CameraScreen(
        actionBarStore = Store(initialState = ActionBarState(avatarName = "Camera")),
        navigationStore = Store(initialState = NavigationState()),
    )
}
