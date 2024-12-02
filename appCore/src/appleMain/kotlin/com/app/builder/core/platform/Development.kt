package com.app.builder.core.platform

actual val developmentMode: Boolean = kotlin.native.Platform.isDebugBinary
