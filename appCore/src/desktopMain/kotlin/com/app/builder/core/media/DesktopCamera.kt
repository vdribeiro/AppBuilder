package com.app.builder.core.media

import com.app.builder.test.ExcludeFromTesting

/** Desktop [Camera] is not supported as there is no standard JVM API for rendering a camera stream without native libraries. */
@ExcludeFromTesting
internal class DesktopCamera: Camera()

internal actual fun createCamera(): Camera = DesktopCamera()
