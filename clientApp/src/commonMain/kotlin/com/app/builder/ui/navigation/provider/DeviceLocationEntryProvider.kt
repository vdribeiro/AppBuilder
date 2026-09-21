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
import com.app.builder.ui.screen.devicelocation.DeviceLocationScreen

/**
 * The device location routes.
 *
 * @param useCases The use cases.
 */
fun EntryProviderScope<NavKey>.deviceLocationProvider(useCases: UseCases) {
    entry<Screen.DeviceLocation> {
        val router = LocalRouter.current
        DeviceLocationScreen(
            actionBarStore = viewModel { ActionBarStore(state = ActionBarState(title = "device_location"), router = router, authenticationUseCases = useCases.authenticationUseCases) },
            navigationStore = viewModel { NavigationStore(state = NavigationState(selected = NavigationRoute.DEVICE_LOCATION), router = router, authenticationUseCases = useCases.authenticationUseCases) },
        )
    }
}
