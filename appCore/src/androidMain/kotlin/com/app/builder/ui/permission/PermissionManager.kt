package com.app.builder.ui.permission

import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.suspendCancellableCoroutine
import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.core.content.ContextCompat
import com.app.builder.applicationContext

/** Android [PermissionManager] implementation backed by [ContextCompat] permission checks and an activity result launcher for runtime permission requests. */
private class AndroidPermissionManager: PermissionManager() {

    /** Launches the runtime permission request for the given manifest permission string. Assigned once the launcher is available in [rememberPermissionManager]. */
    var launch: ((String) -> Unit)? = null
    /** Awaits the result of the in-flight permission request, if any. */
    var pendingResult: CancellableContinuation<Boolean>? = null

    override suspend fun hasPermission(permission: Permission): Boolean {
        super.hasPermission(permission = permission)
        return ContextCompat.checkSelfPermission(applicationContext, permission.toManifestPermission()) == PackageManager.PERMISSION_GRANTED
    }

    override suspend fun requestPermission(permission: Permission): Boolean {
        super.requestPermission(permission = permission)
        val launch = launch ?: return false
        return suspendCancellableCoroutine { continuation ->
            pendingResult = continuation
            launch(permission.toManifestPermission())
        }
    }

    /**
     * Maps a [Permission] to its corresponding Android manifest permission string.
     *
     * @receiver The [Permission] to map.
     * @return The equivalent `android.Manifest.permission` string.
     */
    private fun Permission.toManifestPermission(): String = when (this) {
        Permission.NOTIFICATIONS -> Manifest.permission.POST_NOTIFICATIONS
        Permission.CAMERA -> Manifest.permission.CAMERA
        Permission.LOCATION -> Manifest.permission.ACCESS_FINE_LOCATION
    }
}

@Composable
actual fun rememberPermissionManager(): PermissionManager = remember { AndroidPermissionManager() }.apply {
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { granted ->
            val continuation = pendingResult
            pendingResult = null
            continuation?.resumeWith(result = Result.success(value = granted))
        }
    )
    launch = launcher::launch
}
