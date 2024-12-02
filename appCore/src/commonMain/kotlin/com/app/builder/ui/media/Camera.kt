package com.app.builder.ui.media

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.app.builder.core.media.Camera
import com.app.builder.ui.lifecycle.Register

/**
 * Creates and remembers a [Camera] with a lifecycle.
 *
 * @return [Camera] instance.
 */
@Composable
fun rememberCamera(): Camera = remember { Camera.create() }.also { RegisterCameraLifecycle(camera = it) }

/**
 * Registers a lifecycle callback for camera to stop when the app is in the background and resume when the app is in the foreground.
 *
 * @param camera to register the lifecycle.
 */
@Composable
private fun RegisterCameraLifecycle(camera: Camera) {
    var state by remember { mutableStateOf(value = camera.state.value) }
    Register(
        camera,
        onBackground = {
            state = camera.state.value
            camera.stopPreview()
        },
        onForeground = {
            if (state == Camera.State.Previewing) camera.startPreview()
        },
        onDispose = {
            camera.dispose()
        }
    )
}

/**
 * Renders a [Camera].
 *
 * @param modifier Modifier to be applied.
 * @param camera to use in the preview.
 */
@Composable
expect fun CameraPreview(modifier: Modifier = Modifier, camera: Camera)
