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
import com.app.builder.ui.screen.configs.ConfigScreenState
import com.app.builder.ui.screen.configs.ConfigScreenStore
import com.app.builder.ui.screen.configs.ConfigsScreen

/**
 * The flags and configs route.
 *
 * @param useCases The use cases.
 */
fun EntryProviderScope<NavKey>.configsProvider(useCases: UseCases) {
    entry<Screen.Configs> {
        val router = LocalRouter.current
        ConfigsScreen(
            actionBarStore = viewModel { ActionBarStore(state = ActionBarState(title = "config"), router = router, authenticationUseCases = useCases.authenticationUseCases) },
            navigationStore = viewModel { NavigationStore(state = NavigationState(selected = NavigationRoute.CONFIG), router = router, authenticationUseCases = useCases.authenticationUseCases) },
            store = viewModel { ConfigScreenStore(state = ConfigScreenState(), configUseCases = useCases.configUseCases, authenticationUseCases = useCases.authenticationUseCases) },
        )
    }
}
