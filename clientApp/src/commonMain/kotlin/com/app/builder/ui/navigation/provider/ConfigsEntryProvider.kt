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
import com.app.builder.ui.screen.configs.ConfigType
import com.app.builder.ui.screen.configs.ConfigsScreen

/**
 * The flags and configs routes.
 *
 * @param useCases The use cases.
 */
fun EntryProviderScope<NavKey>.configsProvider(useCases: UseCases) {
    entry<Screen.ClientFlags> {
        val router = LocalRouter.current
        ConfigsScreen(
            actionBarStore = viewModel { ActionBarStore(state = ActionBarState(title = "client_flag"), router = router, authenticationUseCases = useCases.authenticationUseCases) },
            navigationStore = viewModel { NavigationStore(state = NavigationState(selected = NavigationRoute.CLIENT_FLAG), router = router, authenticationUseCases = useCases.authenticationUseCases) },
            store = viewModel { ConfigScreenStore(state = ConfigScreenState(configType = ConfigType.CLIENT_FLAG), configUseCases = useCases.configUseCases) },
        )
    }
    entry<Screen.ClientConfigs> {
        val router = LocalRouter.current
        ConfigsScreen(
            actionBarStore = viewModel { ActionBarStore(state = ActionBarState(title = "client_config"), router = router, authenticationUseCases = useCases.authenticationUseCases) },
            navigationStore = viewModel { NavigationStore(state = NavigationState(selected = NavigationRoute.CLIENT_CONFIG), router = router, authenticationUseCases = useCases.authenticationUseCases) },
            store = viewModel { ConfigScreenStore(state = ConfigScreenState(configType = ConfigType.CLIENT_CONFIG), configUseCases = useCases.configUseCases) },
        )
    }
    entry<Screen.ServerFlags> {
        val router = LocalRouter.current
        ConfigsScreen(
            actionBarStore = viewModel { ActionBarStore(state = ActionBarState(title = "server_flag"), router = router, authenticationUseCases = useCases.authenticationUseCases) },
            navigationStore = viewModel { NavigationStore(state = NavigationState(selected = NavigationRoute.SERVER_FLAG), router = router, authenticationUseCases = useCases.authenticationUseCases) },
            store = viewModel { ConfigScreenStore(state = ConfigScreenState(configType = ConfigType.SERVER_FLAG), configUseCases = useCases.configUseCases) },
        )
    }
    entry<Screen.ServerConfigs> {
        val router = LocalRouter.current
        ConfigsScreen(
            actionBarStore = viewModel { ActionBarStore(state = ActionBarState(title = "server_config"), router = router, authenticationUseCases = useCases.authenticationUseCases) },
            navigationStore = viewModel { NavigationStore(state = NavigationState(selected = NavigationRoute.SERVER_CONFIG), router = router, authenticationUseCases = useCases.authenticationUseCases) },
            store = viewModel { ConfigScreenStore(state = ConfigScreenState(configType = ConfigType.SERVER_CONFIG), configUseCases = useCases.configUseCases) },
        )
    }
}
