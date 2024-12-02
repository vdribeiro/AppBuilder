package com.app.builder.ui.media

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.app.builder.core.media.Camera
import com.app.builder.ui.core.text.Text

@Composable
actual fun CameraPreview(modifier: Modifier, camera: Camera) {
    // Live preview is not supported on desktop as there is no standard JVM API for rendering a webcam stream without native libraries.
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Text(text = "not_supported")
    }
}
