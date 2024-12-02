package com.app.builder.core.media

/** Desktop [Camera] is not supported as there is no standard JVM API for rendering a camera stream without native libraries. */
internal class DesktopCamera: Camera()

internal actual fun createCamera(): Camera = DesktopCamera()
