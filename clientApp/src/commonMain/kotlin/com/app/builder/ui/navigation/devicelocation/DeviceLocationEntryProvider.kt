package com.app.builder.ui.navigation.devicelocation

import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.app.builder.domain.gateway.UseCases
import com.app.builder.ui.component.actionbar.ActionBarState
import com.app.builder.ui.component.actionbar.ActionBarStore
import com.app.builder.ui.component.navigation.NavigationState
import com.app.builder.ui.navigation.LocalRouter
import com.app.builder.ui.navigation.Screen
import com.app.builder.ui.store.Store

/**
 * The device location routes.
 *
 * @param useCases The use cases.
 * @param navigationStore The navigation bar store shared by every screen.
 */
fun EntryProviderScope<NavKey>.deviceLocationProvider(useCases: UseCases, navigationStore: Store<NavigationState, Unit>) {
    entry<Screen.DeviceLocation> {
        val router = LocalRouter.current
        DeviceLocationScreen(
            actionBarStore = viewModel { ActionBarStore(state = ActionBarState(), router = router, authenticationUseCases = useCases.authenticationUseCases, statusUseCases = useCases.statusUseCases) },
            navigationStore = navigationStore,
        )
    }
}
