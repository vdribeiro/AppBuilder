package com.app.builder.ui.navigation.camera

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.app.builder.domain.gateway.UseCases
import com.app.builder.ui.navigation.Screen
import com.app.builder.ui.navigation.hostSessionStore

/**
 * The camera routes.
 *
 * @param useCases The use cases.
 */
fun EntryProviderScope<NavKey>.cameraProvider(useCases: UseCases) {
    entry<Screen.Camera> {
        CameraScreen(
            sessionStore = hostSessionStore(useCases = useCases),
        )
    }
}
