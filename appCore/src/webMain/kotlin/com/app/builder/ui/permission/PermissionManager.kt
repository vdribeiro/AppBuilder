package com.app.builder.ui.permission

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

/** Web [PermissionManager] implementation. The browser prompts for access at the point of use, so all permissions are treated as granted here. */
private class WebPermissionManager: PermissionManager() {
    override suspend fun hasPermission(permission: Permission): Boolean {
        super.hasPermission(permission = permission)
        return true
    }

    override suspend fun requestPermission(permission: Permission): Boolean {
        super.requestPermission(permission = permission)
        return true
    }
}

@Composable
actual fun rememberPermissionManager(): PermissionManager = remember { WebPermissionManager() }
