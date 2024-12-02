package com.app.builder.ui.navigation.provider

import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.app.builder.domain.gateway.UseCases
import com.app.builder.ui.component.navigation.NavigationRoute
import com.app.builder.ui.component.navigation.NavigationState
import com.app.builder.ui.component.navigation.NavigationStore
import com.app.builder.ui.component.useravatar.UserAvatarState
import com.app.builder.ui.component.useravatar.UserAvatarStore
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
            navigationStore = viewModel { NavigationStore(state = NavigationState(selected = NavigationRoute.CLIENT_FLAG), router = router, authenticationUseCases = useCases.authenticationUseCases) },
            userAvatarStore = viewModel { UserAvatarStore(state = UserAvatarState(), authenticationUseCases = useCases.authenticationUseCases) },
            store = viewModel { ConfigScreenStore(state = ConfigScreenState(configType = ConfigType.CLIENT_FLAG), configUseCases = useCases.configUseCases) },
        )
    }
    entry<Screen.ClientConfigs> {
        val router = LocalRouter.current
        ConfigsScreen(
            navigationStore = viewModel { NavigationStore(state = NavigationState(selected = NavigationRoute.CLIENT_CONFIG), router = router, authenticationUseCases = useCases.authenticationUseCases) },
            userAvatarStore = viewModel { UserAvatarStore(state = UserAvatarState(), authenticationUseCases = useCases.authenticationUseCases) },
            store = viewModel { ConfigScreenStore(state = ConfigScreenState(configType = ConfigType.CLIENT_CONFIG), configUseCases = useCases.configUseCases) },
        )
    }
    entry<Screen.ServerFlags> {
        val router = LocalRouter.current
        ConfigsScreen(
            navigationStore = viewModel { NavigationStore(state = NavigationState(selected = NavigationRoute.SERVER_FLAG), router = router, authenticationUseCases = useCases.authenticationUseCases) },
            userAvatarStore = viewModel { UserAvatarStore(state = UserAvatarState(), authenticationUseCases = useCases.authenticationUseCases) },
            store = viewModel { ConfigScreenStore(state = ConfigScreenState(configType = ConfigType.SERVER_FLAG), configUseCases = useCases.configUseCases) },
        )
    }
    entry<Screen.ServerConfigs> {
        val router = LocalRouter.current
        ConfigsScreen(
            navigationStore = viewModel { NavigationStore(state = NavigationState(selected = NavigationRoute.SERVER_CONFIG), router = router, authenticationUseCases = useCases.authenticationUseCases) },
            userAvatarStore = viewModel { UserAvatarStore(state = UserAvatarState(), authenticationUseCases = useCases.authenticationUseCases) },
            store = viewModel { ConfigScreenStore(state = ConfigScreenState(configType = ConfigType.SERVER_CONFIG), configUseCases = useCases.configUseCases) },
        )
    }
}
