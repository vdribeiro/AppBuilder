package com.app.builder.ui.media

import platform.AVFoundation.AVCaptureVideoPreviewLayer
import platform.AVFoundation.AVLayerVideoGravityResizeAspectFill
import platform.UIKit.UIView
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.UIKitView
import com.app.builder.core.media.AppleCamera
import com.app.builder.core.media.Camera

@Composable
actual fun CameraPreview(modifier: Modifier, camera: Camera) {
    val appleCamera = camera as? AppleCamera ?: return
    val session = remember { appleCamera.init() } ?: run {
        appleCamera.dispose()
        return
    }
    DisposableEffect(key1 = Unit) {
        onDispose { appleCamera.dispose() }
    }
    // Use UIKitView to embed a native UIView whose layer hosts an AVCaptureVideoPreviewLayer connected to AppleCamera.session.
    // The preview layer uses AVLayerVideoGravityResizeAspectFill so the video fills the view bounds without letterboxing.
    // The `update` block re-applies the view bounds to the preview layer on every layout pass to keep them in sync.
    UIKitView(
        modifier = modifier,
        factory = {
            UIView().apply {
                val previewLayer = AVCaptureVideoPreviewLayer(session = session).apply {
                    videoGravity = AVLayerVideoGravityResizeAspectFill
                }
                layer.addSublayer(previewLayer)
            }
        },
        update = { view ->
            val previewLayer = view.layer.sublayers?.firstOrNull() as? AVCaptureVideoPreviewLayer
            previewLayer?.frame = view.bounds
        },
    )
}
