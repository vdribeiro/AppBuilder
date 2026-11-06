package com.app.builder.ui.navigation.provider

import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.app.builder.domain.gateway.UseCases
import com.app.builder.ui.component.actionbar.ActionBarState
import com.app.builder.ui.component.actionbar.ActionBarStore
import com.app.builder.ui.component.navigation.NavigationState
import com.app.builder.ui.navigation.LocalRouter
import com.app.builder.ui.navigation.Screen
import com.app.builder.ui.screen.configs.ConfigScreenState
import com.app.builder.ui.screen.configs.ConfigScreenStore
import com.app.builder.ui.screen.configs.ConfigsScreen
import com.app.builder.ui.store.Store

/**
 * The flags and configs route.
 *
 * @param useCases The use cases.
 * @param navigationStore The navigation bar store shared by every screen.
 */
fun EntryProviderScope<NavKey>.configsProvider(useCases: UseCases, navigationStore: Store<NavigationState, Unit>) {
    entry<Screen.Configs> {
        val router = LocalRouter.current
        ConfigsScreen(
            actionBarStore = viewModel { ActionBarStore(state = ActionBarState(), router = router, authenticationUseCases = useCases.authenticationUseCases, statusUseCases = useCases.statusUseCases) },
            navigationStore = navigationStore,
            store = viewModel { ConfigScreenStore(state = ConfigScreenState(), configUseCases = useCases.configUseCases, authenticationUseCases = useCases.authenticationUseCases) },
        )
    }
}
