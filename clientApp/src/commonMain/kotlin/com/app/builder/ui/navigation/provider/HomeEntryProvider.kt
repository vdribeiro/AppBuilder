package com.app.builder.ui.navigation.provider

import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.app.builder.domain.gateway.UseCases
import com.app.builder.ui.component.navigation.NavigationRoute
import com.app.builder.ui.component.navigation.NavigationState
import com.app.builder.ui.component.navigation.NavigationStore
import com.app.builder.ui.navigation.LocalRouter
import com.app.builder.ui.navigation.Screen
import com.app.builder.ui.screen.home.HomeScreen
import com.app.builder.ui.screen.home.HomeScreenState
import com.app.builder.ui.screen.home.HomeScreenStore

/**
 * The home routes.
 *
 * @param useCases The use cases.
 */
fun EntryProviderScope<NavKey>.homeProvider(useCases: UseCases) {
    entry<Screen.Home> {
        val router = LocalRouter.current
        HomeScreen(
            navigationStore = viewModel { NavigationStore(state = NavigationState(selected = NavigationRoute.HOME), router = router, authenticationUseCases = useCases.authenticationUseCases) },
            store = viewModel { HomeScreenStore(state = HomeScreenState(), authenticationUseCases = useCases.authenticationUseCases) },
        )
    }
}
