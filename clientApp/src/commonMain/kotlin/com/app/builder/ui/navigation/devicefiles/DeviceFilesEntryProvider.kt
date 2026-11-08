package com.app.builder.ui.navigation.devicefiles

import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.app.builder.domain.gateway.UseCases
import com.app.builder.ui.navigation.Screen
import com.app.builder.ui.navigation.hostSessionStore

/**
 * The device files routes.
 *
 * @param useCases The use cases.
 */
fun EntryProviderScope<NavKey>.deviceFilesProvider(useCases: UseCases) {
    entry<Screen.DeviceFiles> {
        DeviceFilesScreen(
            sessionStore = hostSessionStore(useCases = useCases),
            store = viewModel { DeviceFilesScreenStore(state = DeviceFilesScreenState()) },
        )
    }
}
