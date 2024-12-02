package com.app.builder.ui.permission

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

/** Desktop [PermissionManager] implementation. Desktop has no runtime permission model, so all permissions are treated as granted. */
private class DesktopPermissionManager: PermissionManager() {
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
actual fun rememberPermissionManager(): PermissionManager = remember { DesktopPermissionManager() }
