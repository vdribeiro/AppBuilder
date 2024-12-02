package com.app.builder.ui.permission

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import androidx.compose.runtime.Composable
import com.app.builder.core.config.ClientFlags
import com.app.builder.core.flow.Dispatcher

/** Manager for checking and requesting system permissions. */
open class PermissionManager {

    /** Isolated scope for in-flight permission requests, decoupled from any single caller's lifecycle. */
    private val scope = CoroutineScope(context = SupervisorJob())
    /** Guards read/write access to [inFlightRequests]. */
    private val mutex = Mutex()
    /** The in-flight request per [Permission], if any, so a re-entrant call awaits it instead of starting a duplicate platform request. */
    private val inFlightRequests = mutableMapOf<Permission, Deferred<Boolean>>()

    /**
     * Checks if the given permission is already granted.
     *
     * @param permission The [Permission] to check.
     * @return True if granted, false otherwise.
     */
    open suspend fun hasPermission(permission: Permission): Boolean = false

    /**
     * Requests the given permission from the user.
     *
     * @param permission The [Permission] to request.
     * @return True if granted, false otherwise.
     */
    protected open suspend fun requestPermission(permission: Permission): Boolean = false

    /**
     * Ensures the [permission] is granted, requesting it if necessary.
     *
     * @param permission The [Permission] to grant.
     * @return true if granted, false otherwise.
     */
    suspend fun grantPermission(permission: Permission): Boolean {
        when (permission) {
            Permission.NOTIFICATIONS -> if (!ClientFlags.flags.notifications) return false
            Permission.CAMERA -> if (!ClientFlags.flags.camera) return false
            Permission.LOCATION -> if (!ClientFlags.flags.locationCapture) return false
        }
        if (hasPermission(permission = permission)) return true
        return mutex.withLock {
            inFlightRequests.getOrPut(key = permission) {
                scope.async(context = Dispatcher.Default) {
                    try {
                        requestPermission(permission = permission)
                    } finally {
                        mutex.withLock { inFlightRequests -= permission }
                    }
                }
            }
        }.await()
    }
}

/**
 * Creates and remembers a [PermissionManager].
 *
 * @return [PermissionManager] instance.
 */
@Composable
expect fun rememberPermissionManager(): PermissionManager
