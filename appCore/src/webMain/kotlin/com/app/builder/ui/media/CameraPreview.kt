package com.app.builder.ui.media

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.app.builder.core.media.Camera
import com.app.builder.core.media.WebCamera

@Composable
actual fun CameraPreview(modifier: Modifier, camera: Camera) {
    val webCamera = camera as? WebCamera ?: return
    remember { webCamera.init() } ?: run {
        webCamera.dispose()
        return
    }
    DisposableEffect(key1 = Unit) {
        onDispose { webCamera.dispose() }
    }
}
