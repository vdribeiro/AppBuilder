package com.app.builder.ui.navigation.camera

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.app.builder.ui.component.session.SessionState
import com.app.builder.ui.navigation.Screen
import com.app.builder.ui.store.Store

/**
 * The camera routes.
 *
 * @param sessionStore The session store shared by every screen.
 */
fun EntryProviderScope<NavKey>.cameraProvider(sessionStore: Store<SessionState, Unit>) {
    entry<Screen.Camera> {
        CameraScreen(
            sessionStore = sessionStore,
        )
    }
}
