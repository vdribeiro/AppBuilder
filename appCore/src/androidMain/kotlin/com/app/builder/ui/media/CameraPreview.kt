package com.app.builder.ui.media

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.app.builder.core.media.AndroidCamera
import com.app.builder.core.media.Camera

@Composable
actual fun CameraPreview(modifier: Modifier, camera: Camera) {
    val androidCamera = camera as? AndroidCamera ?: return
    val lifecycleOwner = LocalLifecycleOwner.current
    key(lifecycleOwner) {
        val previewView = remember { androidCamera.init(lifecycleOwner = lifecycleOwner) } ?: run {
            androidCamera.dispose()
            return
        }
        DisposableEffect(key1 = Unit) {
            onDispose { androidCamera.dispose() }
        }
        AndroidView(modifier = modifier, factory = { previewView })
    }
}
