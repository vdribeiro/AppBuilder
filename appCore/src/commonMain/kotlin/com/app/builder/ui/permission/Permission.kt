package com.app.builder.ui.permission

import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.staticCompositionLocalOf

/** CompositionLocal providing the current [PermissionManager]. */
val LocalPermissionManager: ProvidableCompositionLocal<PermissionManager> = staticCompositionLocalOf { PermissionManager() }

/** Defines the types of permissions that can be requested across platforms. */
enum class Permission {
    NOTIFICATIONS,
    CAMERA,
    LOCATION
}
