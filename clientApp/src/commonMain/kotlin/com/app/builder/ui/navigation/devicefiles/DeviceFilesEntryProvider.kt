package com.app.builder.ui.navigation.devicefiles

import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.app.builder.ui.component.session.SessionState
import com.app.builder.ui.navigation.Screen
import com.app.builder.ui.store.Store

/**
 * The device files routes.
 *
 * @param sessionStore The session store shared by every screen.
 */
fun EntryProviderScope<NavKey>.deviceFilesProvider(sessionStore: Store<SessionState, Unit>) {
    entry<Screen.DeviceFiles> {
        DeviceFilesScreen(
            sessionStore = sessionStore,
            store = viewModel { DeviceFilesScreenStore(state = DeviceFilesScreenState()) },
        )
    }
}
