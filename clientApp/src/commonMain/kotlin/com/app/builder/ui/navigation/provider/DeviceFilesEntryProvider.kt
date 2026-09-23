package com.app.builder.ui.navigation.provider

import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.app.builder.domain.gateway.UseCases
import com.app.builder.ui.component.actionbar.ActionBarState
import com.app.builder.ui.component.actionbar.ActionBarStore
import com.app.builder.ui.component.navigation.NavigationRoute
import com.app.builder.ui.component.navigation.NavigationState
import com.app.builder.ui.component.navigation.NavigationStore
import com.app.builder.ui.navigation.LocalRouter
import com.app.builder.ui.navigation.Screen
import com.app.builder.ui.screen.devicefiles.DeviceFilesScreen
import com.app.builder.ui.screen.devicefiles.DeviceFilesScreenState
import com.app.builder.ui.screen.devicefiles.DeviceFilesScreenStore

/**
 * The device files routes.
 *
 * @param useCases The use cases.
 */
fun EntryProviderScope<NavKey>.deviceFilesProvider(useCases: UseCases) {
    entry<Screen.DeviceFiles> {
        val router = LocalRouter.current
        DeviceFilesScreen(
            actionBarStore = viewModel { ActionBarStore(state = ActionBarState(title = "device_file"), router = router, authenticationUseCases = useCases.authenticationUseCases) },
            navigationStore = viewModel { NavigationStore(state = NavigationState(selected = NavigationRoute.DEVICE_FILE), router = router, authenticationUseCases = useCases.authenticationUseCases) },
            store = viewModel { DeviceFilesScreenStore(state = DeviceFilesScreenState()) },
        )
    }
}
